package club.craftcoder.vicuna.core

object Container {

  private[vicuna] val TRANSFER_CONTAINER = collection.mutable.Map[String, collection.mutable.Map[String, List[TransferDef]]]()

  private[vicuna] val GRAPH_CONTAINER = collection.mutable.Map[String, GraphDef]()

  private[vicuna] var dataExchange: DataExchange = _
}
