package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  *
  * The FindSuccessor algorithm is defined in the Chord paper as follows:
  *
  * {{{
  *   n.find_successor(id)
  *     n' = find_predecessor(id)
  *     return n'.successor;
  * }}}
  *
  *
  */
object FindSuccessor
{
  def run(kNodeId: Int, newNodeId: Int, nodesObj: Array[Node]): Int =
  {
    //n' = find_predecessor(id)
    val tempNode = nodesObj(FindPredecessor.run(kNodeId, newNodeId,nodesObj)).succ
    return tempNode //return n'.successor;
  }
}
