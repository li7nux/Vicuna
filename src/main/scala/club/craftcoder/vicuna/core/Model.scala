package club.craftcoder.vicuna.core

import com.ecfront.common.Resp

import scala.collection.mutable.ArrayBuffer

case class StatusDef(
                      code: String,
                      name: String,
                      execFun: FlowInst => Resp[Void]
                    )

case class TransitionDef(
                        fromCode: String,
                        toCode: String,
                        auto: Boolean,
                        condition: FlowInst => Boolean = null,
                        expireSec: Int = -1
                      )

case class GraphDef(code: String,
                    name: String,
                    startNodeCode: String,
                    nodes: collection.mutable.Map[String, NodeDef]
                   )

case class NodeDef(code: String,
                   name: String,
                   execFun: FlowInst => Resp[Void],
                   parentNodeCodes: ArrayBuffer[String],
                   childrenNodeCodes: ArrayBuffer[String]
                  )

case class FlowInst(
                flowCode: String,
                objCode: String,
                currStatusCodes: Set[String],
                currArgs: collection.mutable.Map[String, Any]
              ){

  var objName: String=_

}