package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  *
  *
  * The UpdateFingerTable algorithm is defined in the Chord paper as follows:
  *
  * n.update_finger_table(s,i)
  *   if (s belongs to [n,finger[i].node))
  *     finger[i].node = s
  *     p = predecessor
  *     p.update_finger_table(s,i)
  *
  * In this implementation the recursion is ridded off using auxiliary data structures and more computational time
  *
  *
  */
object UpdateFingerTableAlg {

  def run(toBeUpdatedNodes: List[Int], newValue: Int, node: Node) =
  {
    //same thing as belongs, exloitation of the string
    for (i <- 0 to toBeUpdatedNodes.length - 1) {
      if (node.fingerTableStart.contains(toBeUpdatedNodes(i))) {
        //finger[i].node = s
        node.fingerTable(node.fingerTableStart.indexOf(toBeUpdatedNodes(i))) = node.fingerTable(node.fingerTableStart.indexOf(toBeUpdatedNodes(i))).split(",")(0) + "," + newValue
      }

    }
    //update the start and the node of the auxiliary data structures
    for (i <- 0 to node.keyspace - 1) {
      node.fingerTableStart(i) = node.fingerTable(i).split(",")(0).toInt
      node.fingerTableNode(i) = node.fingerTable(i).split(",")(1).toInt
    }
  }
}