package club.craftcoder.vicuna.core

import java.util.Date

import com.ecfront.common.Resp

import scala.beans.BeanProperty

case class StatusDef(code:String,name:String,execFun: (NodeInst,Any) => Resp[Void])
case class TransferDef(fromCode:String,toCode:String,auto:Boolean,condition: NodeInst => Boolean=null,expireSec:Int= -1)

class NodeInst{

 @BeanProperty var flow_code:String=_
 @BeanProperty var obj_code:String=_
 @BeanProperty var obj_name:String=_
 @BeanProperty var args:collection.Map[String,Any]=_

}

class TransferInst{

  @BeanProperty var obj_code:String=_
  @BeanProperty var from_node_code:String=_
  @BeanProperty var to_node_code:String=_
  @BeanProperty var transfer_operator: String=_
  @BeanProperty var transfer_opt_time: Date=_
  @BeanProperty var args: Map[String,String]=_

}