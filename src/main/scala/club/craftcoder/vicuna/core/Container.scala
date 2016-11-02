package club.craftcoder.vicuna.core

object Container {

  private[vicuna] val TRANSITION_CONTAINER = collection.mutable.Map[String, collection.mutable.Map[String, List[TransitionDef]]]()

  private[vicuna] val GRAPH_CONTAINER = collection.mutable.Map[String, GraphDef]()

  private[vicuna] var dataExchange: DataExchange = _
}
