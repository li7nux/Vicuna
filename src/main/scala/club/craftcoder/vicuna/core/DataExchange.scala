package club.craftcoder.vicuna.core

trait DataExchange {

 private[vicuna] def innerGetFlowInst(flowCode: String, objCode: String): FlowInst = {
    tryLock[FlowInst](flowCode + "_" + objCode) {
      getFlowInst(flowCode, objCode)
    }
  }

  def getFlowInst(flowCode: String, objCode: String): FlowInst

  def disableOldStatus(flowCode: String, currNodeCode:String,objCode: String): Unit

  def preExecute(currNodeCode:String,flowInst: FlowInst): Unit

  def postExecute(currNodeCode:String,flowInst: FlowInst): Unit

  def tryLock[E](code: String)(execFun: => E):E

}
