package club.craftcoder.vicuna.core

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.mutable.ArrayBuffer

object FlowController extends LazyLogging {

  object Manage {

    def generate(flowCode: String, flowName: String, status: List[StatusDef], transitions: List[TransitionDef]): Resp[Void] = {
      logger.info(s"[STATUS] generate flow at $flowCode")
      generateTransitions(flowCode, transitions)
      generateGraph(flowCode, flowName, status, transitions)
      Resp.success(null)
    }

    /**
      * 生成转换容器
      */
    private def generateTransitions(flowCode: String, transitions: List[TransitionDef]): Unit = {
      Container.TRANSITION_CONTAINER += flowCode -> (collection.mutable.Map() ++ transitions.groupBy(_.fromCode).map {
        transitionExt =>
          transitionExt._1 -> transitionExt._2
      })
    }

    /**
      * 生成图
      */
    private def generateGraph(flowCode: String, flowName: String, status: List[StatusDef], transitions: List[TransitionDef]): Unit = {
      val vertexNode = status.find(stat => !Container.TRANSITION_CONTAINER.contains(stat.code)).get
      Container.GRAPH_CONTAINER += flowCode -> GraphDef(flowCode, flowName, vertexNode.code, vertexNode.name, generateNodes(status, transitions))
    }

    /**
      * 生成节点
      */
    private def generateNodes(status: List[StatusDef], transitions: List[TransitionDef]): collection.mutable.Map[String, NodeDef] = {
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
    logger.info(s"[STATUS] start flow : $flowCode , obj : $objCode - $objName")
    val graphF = Container.GRAPH_CONTAINER.get(flowCode)
    if (graphF.isDefined) {
      execute(graphF.get, graphF.get.startNodeCode, null, objCode)
    } else {
      logger.error(s"[STATUS] not found flow [$flowCode]")
      Resp.notFound(s"not found flow [$flowCode]")
    }
  }

  def next(flowCode: String, currNodeCode: String, objCode: String, args: Map[String, Any]): Resp[Void] = {
    logger.info(s"[STATUS] next node from [$currNodeCode] obj $objCode at $flowCode")
    val graphF = Container.GRAPH_CONTAINER.get(flowCode)
    if (graphF.isDefined) {
      val currNodeF = graphF.get.nodes.get(currNodeCode)
      if (currNodeF.isDefined) {
        val flowInst = getFlowInst(flowCode, objCode)
        flowInst.currArgs ++= args
        tryNext(graphF.get, currNodeF.get, flowInst, force = true)
      } else {
        logger.error(s"[STATUS] not found node [${currNodeF.get}] obj $objCode at ${graphF.get.code}")
        Resp.notFound(s"not found node [${currNodeF.get}] obj $objCode at ${graphF.get.code}")
      }
    } else {
      logger.error(s"[STATUS] not found flow [$flowCode]")
      Resp.notFound(s"not found flow [$flowCode]")
    }
  }

  private def tryNext(graph: GraphDef, currNode: NodeDef, flowInst: FlowInst, force: Boolean = false): Resp[Void] = {
    if (flowInst.currStatusCodes.contains(currNode.code)) {
      if (currNode.childrenNodeCodes != null && currNode.childrenNodeCodes.nonEmpty) {
        val transitionF = Container.TRANSITION_CONTAINER(flowInst.flowCode).get(currNode.code)
        if (transitionF.isDefined) {
          transitionF.get.foreach {
            transition =>
              if (transition.auto || force) {
                if (transition.condition == null || transition.condition(flowInst)) {
                  // Next
                  logger.info(s"[STATUS] do next node from [${currNode.code}] to [${transition.toCode}] obj ${flowInst.objCode} at ${graph.code}")
                  execute(graph, transition.toCode, currNode.code, flowInst.objCode)
                }
              }
          }
        } else {
          logger.error(s"[STATUS] not found transition [${currNode.code}] obj ${flowInst.objCode} at ${graph.code}")
        }
        Resp.success(null)
      } else {
        // EOF
        logger.info(s"[STATUS] flow finish obj ${flowInst.objCode} at ${graph.code}")
        Resp.success(null)
      }
    } else {
      logger.error(s"[STATUS] the node : ${currNode.code} is not in current status(${flowInst.currStatusCodes.mkString(",")}) obj ${flowInst.objCode} at ${graph.code}")
      Resp.conflict(s"the node : ${currNode.code} is not in current status(${flowInst.currStatusCodes.mkString(",")}) obj ${flowInst.objCode} at ${graph.code}")
    }
  }

  private def execute(graph: GraphDef, currNodeCode: String, oldNodeCode: String, objCode: String): Resp[Void] = {
    val nodeF = graph.nodes.get(currNodeCode)
    if (nodeF.isDefined) {
      var flowInst = getFlowInst(graph.code, objCode)
      Container.dataExchange.preExecute(currNodeCode, flowInst)
      try {
        val execResult = nodeF.get.execFun(flowInst)
        if (execResult) {
          if (oldNodeCode != null) {
            Container.dataExchange.disableOldStatus(graph.code, oldNodeCode, objCode)
          }
          Container.dataExchange.postExecute(currNodeCode, flowInst)
          flowInst = getFlowInst(graph.code, objCode)
          tryNext(graph, nodeF.get, flowInst)
        } else {
          logger.error(s"[STATUS] execute [$currNodeCode] error obj $objCode at ${graph.code}")
          Resp.unknown(s"execute [$currNodeCode] error obj $objCode at ${graph.code}")
        }
      } catch {
        case e: Throwable =>
          logger.error(s"[STATUS] execute [$currNodeCode] error obj $objCode at ${graph.code} : ${e.getMessage}", e)
          Resp.unknown(s"execute [$currNodeCode] error obj $objCode at ${graph.code} : ${e.getMessage}")
      }
    } else {
      logger.error(s"[STATUS] not found node [$currNodeCode] obj $objCode at ${graph.code}")
      Resp.notFound(s"not found node [$currNodeCode] obj $objCode at ${graph.code}")
    }
  }

  private def getFlowInst(flowCode: String, objCode: String): FlowInst = {
    var flowInst = Container.dataExchange.innerGetFlowInst(flowCode, objCode)
    if (flowInst == null) {
      flowInst = FlowInst(flowCode, objCode, Set(), collection.mutable.Map())
      flowInst.objName = Container.GRAPH_CONTAINER(flowCode).name
    }
    flowInst
  }

}
