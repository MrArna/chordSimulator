package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  *
  *  The FindPredecessor algorithm is defined in the Chord paper as follows:
  *
  * {{{
  *   n.find_predecessor(id)
  *     n' = n;
  *     while (id NOT_IN (n', n'.successor])
  *       n' = n'.closest_preceding_finger(id);
  *     return n';
  * }}}
  *
  */
object FindPredecessor
{
  //definition of a (] interval
  private def InIntervalExInc(Id: Int, nid: Int, succId: Int): Boolean = {
    if (nid < succId) {
      if (Id > nid && Id <= succId) { return true }
      else return false
    } else {
      if (Id > nid || Id <= succId) { return true }
      else return false
    }
  }


  def run(kNodeId: Int, newNodeId: Int, nodesObj: Array[Node]): Int =
  {
    //n' = n;
    var tempNodenodeId = nodesObj(kNodeId).identifier
    //n'.successor]
    var tempNodesuccessor = nodesObj(kNodeId).fingerTable(0).split(",")(1).toInt

    //while (id NOT_IN (n', n'.successor])
    while (!InIntervalExInc(newNodeId, tempNodenodeId, tempNodesuccessor)) {
      //n' = n'.closest_preceding_finger(id);
      tempNodenodeId = ClosestPrecedingFinger.run(tempNodenodeId, newNodeId,nodesObj)
      //n'.successor updating
      tempNodesuccessor = nodesObj(tempNodenodeId).fingerTable(0).split(",")(1).toInt
    }
    return tempNodenodeId // return n';
  }
}
