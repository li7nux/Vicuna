package club.craftcoder.vicuna.core

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.mutable.ArrayBuffer

object FlowController extends LazyLogging {

  object Manage {

    def generate(flowCode: String, flowName: String, status: List[StatusDef], transitions: List[TransferDef]): Resp[Void] = {
      generateTransitions(flowCode, transitions)
      generateGraph(flowCode, flowName, status, transitions)
      Resp.success(null)
    }

    /**
      * 生成转换容器
      */
    private def generateTransitions(flowCode: String, transitions: List[TransferDef]): Unit = {
      Container.TRANSFER_CONTAINER += flowCode -> (collection.mutable.Map() ++ transitions.groupBy(_.fromCode).map {
        transitionExt =>
          transitionExt._1 -> transitionExt._2
      })
    }

    /**
      * 生成图
      */
    private def generateGraph(flowCode: String, flowName: String, status: List[StatusDef], transitions: List[TransferDef]): Unit = {
      val vertexNode = status.find(stat => !Container.TRANSFER_CONTAINER.contains(stat.code)).get
      Container.GRAPH_CONTAINER += flowCode -> GraphDef(flowCode, flowName, vertexNode.code, generateNodes(status, transitions))
    }

    /**
      * 生成节点
      */
    private def generateNodes(status: List[StatusDef], transitions: List[TransferDef]): collection.mutable.Map[String, NodeDef] = {
      collection.mutable.Map() ++ status.map {
        stat =>
          val parentNodeCodes = ArrayBuffer() ++ transitions.filter(_.toCode == stat.code).map(_.fromCode).distinct
          val childrenNodeCodes = ArrayBuffer() ++ transitions.filter(_.fromCode == stat.code).map(_.toCode).distinct
          val node = NodeDef(stat.code, stat.name, stat.execFun, parentNodeCodes, childrenNodeCodes)
          stat.code -> node
      }.toMap
    }

  }

  def start(flowCode: String, objCode: String, objName: String): Resp[Void] = {
    val graphF = Container.GRAPH_CONTAINER.get(flowCode)
    if (graphF.isDefined) {
      execute(graphF.get, graphF.get.startNodeCode, objCode)
    } else {
      Resp.notFound(s"Not found flow [$flowCode]")
    }
  }

  def next(flowCode: String, currNodeCode: String, objCode: String): Resp[Void] = {
    val graphF = Container.GRAPH_CONTAINER.get(flowCode)
    if (graphF.isDefined) {
      val currNodeF = graphF.get.nodes.get(currNodeCode)
      if (currNodeF.isDefined) {
        tryNext(graphF.get, currNodeF.get, objCode)
      } else {
        Resp.notFound(s"Not found node [${currNodeF.get}] in ${graphF.get.code}")
      }
    } else {
      Resp.notFound(s"Not found flow [$flowCode]")
    }
  }

  private def tryNext(graph: GraphDef, currNode: NodeDef, objCode: String): Resp[Void] = {
    if (currNode.childrenNodeCodes != null && currNode.childrenNodeCodes.nonEmpty) {
      currNode.childrenNodeCodes.foreach {
        childNodeCode =>
          val transferF = Container.TRANSFER_CONTAINER(graph.code).get(childNodeCode)
          if (transferF.isDefined) {
            transferF.get.foreach {
              transfer =>
                if (transfer.auto) {
                  val flowInst = getFlowInst(graph.code, currNode.code, objCode)
                  if (transfer.condition==null || transfer.condition(flowInst)) {
                    // Next
                    Container.dataExchange.disableOldStatus(graph.code, currNode.code, objCode)
                    execute(graph, transfer.toCode, objCode)
                  }
                }
            }
          } else {
            // Not found
            // TODO
          }
      }
    } else {
      // EOF
    }
    Resp.success(null)
  }

  private def getFlowInst(flowCode: String, nodeCode: String, objCode: String): FlowInst = {
    var flowInst = Container.dataExchange.innerGetFlowInst(flowCode, objCode)
    if (flowInst == null) {
      flowInst = FlowInst(flowCode, objCode, Set(nodeCode), collection.mutable.Map())
      flowInst.objName=Container.GRAPH_CONTAINER(flowCode).name
    }
    flowInst
  }

  private def execute(graph: GraphDef, nodeCode: String, objCode: String): Resp[Void] = {
    val nodeF = graph.nodes.get(nodeCode)
    if (nodeF.isDefined) {
      var flowInst = getFlowInst(graph.code, nodeCode, objCode)
      Container.dataExchange.preExecute(nodeCode, flowInst)
      try {
        val execResult = nodeF.get.execFun(flowInst)
        if (execResult) {
          flowInst = getFlowInst(graph.code, nodeCode, objCode)
          Container.dataExchange.postExecute(nodeCode, flowInst)
          tryNext(graph, nodeF.get, flowInst.objCode)
        } else {
          Resp.unknown(s"Execute [$nodeCode] error")
        }
      } catch {
        case e: Throwable =>
          Resp.unknown(s"Execute [$nodeCode] error : ${e.getMessage}")
      }
    } else {
      Resp.notFound(s"Not found node [$nodeCode] in ${graph.code}")
    }
  }


}
