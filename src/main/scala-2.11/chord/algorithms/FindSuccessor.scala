package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  */
object FindSuccessor
{
  def run(kNodeId: Int, newNodeId: Int, nodesObj: Array[Node]): Int =
  {
    val tempNode = nodesObj(FindPredecessor.run(kNodeId, newNodeId,nodesObj)).succ
    return tempNode
  }
}
