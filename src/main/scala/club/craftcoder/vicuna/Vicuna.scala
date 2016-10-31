package club.craftcoder.vicuna

import club.craftcoder.vicuna.core._
import com.ecfront.common.Resp

object Vicuna {

  def setDataExchange(dataExchange: DataExchange): this.type = {
    Container.dataExchange = dataExchange
    this
  }

  def define(flowCode: String, flowName: String, status: List[StatusDef], transitions: List[TransferDef]): this.type = {
    FlowController.Manage.generate(flowCode, flowName, status, transitions)
    this
  }

  def start(flowCode: String, objCode: String, objName: String): Resp[Void] = {
    FlowController.start(flowCode, objCode, objName)
  }

  def next(flowCode: String, currNodeCode: String, objCode: String): Resp[Void] = {
    FlowController.next(flowCode, currNodeCode, objCode)
  }

}
