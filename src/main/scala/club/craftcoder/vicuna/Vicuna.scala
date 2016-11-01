package club.craftcoder.vicuna

import club.craftcoder.vicuna.core._
import club.craftcoder.vicuna.ext.ChartController
import com.ecfront.common.Resp

object Vicuna {

  def setDataExchange(dataExchange: DataExchange): this.type = {
    Container.dataExchange = dataExchange
    this
  }

  def define(flowCode: String, flowName: String, status: List[StatusDef], transitions: List[TransitionDef]): this.type = {
    FlowController.Manage.generate(flowCode, flowName, status, transitions)
    this
  }

  def buildChart(flowCode: String,path:String): this.type = {
    ChartController.buildChart(flowCode,path)
    this
  }

  def start(flowCode: String, objCode: String, objName: String): Resp[Void] = {
    FlowController.start(flowCode, objCode, objName)
  }

  def next(flowCode: String, currNodeCode: String, objCode: String,args:Map[String,Any]=Map()): Resp[Void] = {
    FlowController.next(flowCode, currNodeCode, objCode,args)
  }

}
