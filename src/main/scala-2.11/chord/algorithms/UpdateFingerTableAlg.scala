package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  */
object UpdateFingerTableAlg {

  def run(aNodes: List[Int], newValue: Int, node: Node) =
  {
    for (i <- 0 to node.keyspace - 1) {
      node.fingerTableStart(i) = node.fingerTable(i).split(",")(0).toInt
      node.fingerTableNode(i) = node.fingerTable(i).split(",")(1).toInt
    }
    for (i <- 0 to aNodes.length - 1) {
      if (node.fingerTableStart.contains(aNodes(i))) {
        node.fingerTable(node.fingerTableStart.indexOf(aNodes(i))) = node.fingerTable(node.fingerTableStart.indexOf(aNodes(i))).split(",")(0) + "," + newValue

      }

    }

    for (i <- 0 to node.keyspace - 1) {
      node.fingerTableStart(i) = node.fingerTable(i).split(",")(0).toInt
      node.fingerTableNode(i) = node.fingerTable(i).split(",")(1).toInt
    }
  }
}