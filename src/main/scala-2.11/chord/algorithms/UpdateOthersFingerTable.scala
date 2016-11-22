package chord.algorithms

import chord.Node
import chord.Node.UpdateFingerTable

/**
  * Created by Marco on 21/11/16.
  *
  * The UpdateOthers algorithm is defined in the Chord paper as follows:
  *
  * n.update_others()
  *   for i = 1 to m
  *     p = find_predecessor(n - 2^^(i-1))
  *     p.update_finger_table_(n,i)
  *
  * In this implementation the recursion in update finger table is avoided, increasing the computational time
  *
  *
  */
object UpdateOthersFingerTable
{
  def run(node: Node) =
  {

    var affectedNodes: List[Int] = List()
    //creates a list of all possible affected nodes

    //take care of the ring
    if (node.identifier < node.pred) { // the affected nodes are those between pred and last element of the ring + those from 0 to the node
      for (i <- node.pred + 1 to math.pow(2, node.fingerTable.length).toInt - 1) {
        affectedNodes ::= i
      }
      for (i <- 0 to node.identifier) {
        affectedNodes ::= i
      }
    } else { //the affected nodes are those between predecessor and the current node
      for (i <- node.pred + 1 to node.identifier) {
        affectedNodes ::= i
      }
    }

    //notify nodes of the update
    for (i <- 0 to node.nodes.length - 1) {
      if (node.nodes(i) != null) {
        node.nodes(i) ! UpdateFingerTable(affectedNodes, node.identifier)
      }
    }
  }
}
