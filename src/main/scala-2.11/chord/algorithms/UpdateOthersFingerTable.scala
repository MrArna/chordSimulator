package chord.algorithms

import chord.Node
import chord.Node.UpdateFingerTable

/**
  * Created by Marco on 21/11/16.
  */
object UpdateOthersFingerTable
{
  def run(node: Node) =
  {
    var affectedNodes: List[Int] = null
    if (node.identifier < node.pred) {
      affectedNodes = List()
      for (i <- node.pred + 1 to math.pow(2, node.fingerTable.length).toInt - 1) {
        affectedNodes ::= i
      }
      for (i <- 0 to node.identifier) {
        affectedNodes ::= i
      }
    } else {
      affectedNodes = List()
      for (i <- node.pred + 1 to node.identifier) {
        affectedNodes ::= i
      }
    }

    for (i <- 0 to node.nodes.length - 1) {
      if (node.nodes(i) != null) {
        node.nodes(i) ! UpdateFingerTable(affectedNodes, node.identifier)
      }
    }
  }
}
