package club.craftcoder.vicuna.core

import java.lang.Long
import java.util.{Date, UUID}

import com.asto.ybmap.commander.entity.Task_Exec_Log._
import com.asto.ybmap.commander.entity.{Task, Task_Exec_Log}
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.service.masterslave.{Assigner, TaskFinishDTO, TaskPrepareDTO, TaskStartDTO}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Handler

import scala.collection.mutable.ArrayBuffer

/**
  * 工作流引擎
  */
object WorkFlowProcessor extends LazyLogging {

  /**
    * 流程入口方法，多由调度器触发
    *
    */
  def process(task: Task, instanceParameters: Map[String, Any] = Map()): String = {
    prepareTask(task, instanceParameters)
  }

  private def prepareTask(task: Task, instanceParameters: Map[String, Any]): String = {
    val taskLog = Task_Exec_Log()
    taskLog.id = UUID.randomUUID().toString
    taskLog.task_code = task.code
    taskLog.task_version = task.version
    taskLog.name = task.name + '.' + task.version + System.currentTimeMillis()
    taskLog.exec_status = STATUS_PREPARE
    taskLog.start_time = 0
    taskLog.finish_time = 0
    taskLog.exec_time = 0
    taskLog.result_msg = ""
    Task_Exec_Log.save(taskLog)
    logger.info(
      s"""
         |===== Workflow : Prepare task instance : ${task.code}.${task.version} @ ${taskLog.id}
         |""".stripMargin)
    // 调用MQ，通知相应的worker组件处理
    Assigner.Master.prepareTask(TaskPrepareDTO(
      instanceId = taskLog.id,
      worker = task.worker,
      category = task.category,
      taskInfo = task.taskInfo,
      taskVar = task.variable,
      instanceParameters = instanceParameters
    ))
    taskLog.id
  }

  def startTask(dto: TaskStartDTO): Unit = {
    val logR = Task_Exec_Log.getById(dto.instanceId)
    if (logR && logR.body != null) {
      val log = logR.body
      log.start_time = TimeHelper.msf.format(new Date()).toLong
      log.exec_status = STATUS_START
      Task_Exec_Log.update(log)
      logger.info(
        s"""===== Workflow : Start task instance : ${log.task_code}.${log.task_version} @ ${log.id}""")
    } else {
      logger.error("Task Exec Log NOT exist :" + dto.instanceId)
    }
  }

  def finishTask(dto: TaskFinishDTO): Unit = {
    val logR = Task_Exec_Log.getById(dto.instanceId)
    if (logR && logR.body != null) {
      val log = logR.body
      log.finish_time = TimeHelper.msf.format(new Date()).toLong
      log.exec_time = log.finish_time - log.start_time
      log.result_msg = dto.message
      log.exec_status = if (dto.isSuccess) {
        if (dto.hasChange) {
          STATUS_SUCCESS
        } else {
          STATUS_NO_CHANGE
        }
      } else {
        STATUS_FAIL
      }
      val taskR = Task.get(log.task_code, log.task_version)
      val task = taskR.body
      if (log.exec_status == STATUS_SUCCESS && dto.taskVar != null) {
        task.variable = dto.taskVar
        Task.update(task)
      }
      Task_Exec_Log.update(log)
      logger.info(
        s"""===== Workflow : Finish task instance : ${log.task_code}.${log.task_version} @ ${log.id}""")
      if (dto.isSuccess && dto.hasChange) {
        execDependentTasks(task.id, dto.instanceParameters)
      }
    } else {
      logger.error("Task Exec Log NOT exist :" + dto.instanceId)
    }
  }

  private def execDependentTasks(id: String, instanceParameters: Map[String, Any]): Unit = {
    val currentTaskR = Task.getById(id)
    val currentTask = currentTaskR.body
    val dTasksR = Task.findDependentTasks(id)
    if (dTasksR.body.isEmpty) {
      logger.info(
        s"""The task not exist dependent Tasks """
      )
    } else {
      dTasksR.body.foreach {
        dTask =>
          if (dTask.dependentStrategy == Task.DEPENDENT_STRATEGY_OR) {
            prepareTask(dTask, instanceParameters)
          } else if (dTask.dependentStrategy == Task.DEPENDENT_STRATEGY_AND) {
            this.synchronized {
              if (!currentWaitingExecTasks.contains(dTask.id)) {
                currentWaitingExecTasks += dTask.id -> dTask.dependentTasks.to[ArrayBuffer]
              }
              currentWaitingExecTasks(dTask.id) -= currentTask.code + "." + currentTask.version
              if (currentWaitingExecTasks(dTask.id).isEmpty) {
                currentWaitingExecTasks -= dTask.id
                prepareTask(dTask, instanceParameters)
              }
            }
          } else {
            this.synchronized {
              if (!currentDelayExecTasks.contains(dTask.id)) {
                currentDelayExecTasks += dTask.id
                EZContext.vertx.setTimer(dTask.dependentStrategy, new Handler[Long] {
                  override def handle(event: Long): Unit = {
                    currentDelayExecTasks -= dTask.id
                    prepareTask(dTask, instanceParameters)
                  }
                })
              }
            }
          }
      }
    }
  }

  private val currentWaitingExecTasks = collection.mutable.Map[String, ArrayBuffer[String]]()
  private val currentDelayExecTasks = ArrayBuffer[String]()

}
