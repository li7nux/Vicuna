package club.craftcoder.vicuna

import java.util.Date

import club.craftcoder.vicuna.core.{DataExchange, FlowInst}
import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.helper.TimeHelper

object TestDataExchange extends DataExchange {

  override def getFlowInst(flowCode: String, objCode: String): FlowInst = {
    val logs = Vic_status_log.findEnabled("flow_code = ? AND obj_code = ?", List(flowCode, objCode)).body
    if (logs.nonEmpty) {
      FlowInst(flowCode, objCode, logs.map(_.status).toSet, collection.mutable.Map(
        "down_payment" -> logs.head.down_payment,
        "final_payment" -> logs.head.final_payment
      ))
    } else {
      null
    }
  }

  override def disableOldStatus(flowCode: String, currNodeCode: String, objCode: String): Unit = {
    Vic_status_log.disableByCond("flow_code = ? AND obj_code = ? AND status =?",
      List(flowCode, objCode, currNodeCode))
  }

  override def preExecute(currNodeCode: String, flowInst: FlowInst): Unit = {

  }

  override def postExecute(currNodeCode: String, flowInst: FlowInst): Unit = {
    val log = new Vic_status_log
    log.status = currNodeCode
    log.flow_code = flowInst.flowCode
    log.obj_code = flowInst.objCode
    log.add_time = TimeHelper.msf.format(new Date()).toLong
    log.down_payment = flowInst.currArgs("down_payment").asInstanceOf[BigDecimal]
    log.final_payment = flowInst.currArgs("final_payment").asInstanceOf[BigDecimal]
    log.enable = true
    Vic_status_log.save(log)
  }

  override def tryLock[E](code: String)(execFun: => E): E = {
    var result: Any = null
    EZ.dist.lock(code).tryLockWithFun(Integer.MAX_VALUE) {
      result = execFun
    }
    result.asInstanceOf[E]
  }

}
