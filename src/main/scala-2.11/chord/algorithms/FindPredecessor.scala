package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  */
object FindPredecessor
{

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
    var tempNodenodeId = nodesObj(kNodeId).identifier
    var tempNodesuccessor = nodesObj(kNodeId).fingerTable(0).split(",")(1).toInt

    while (!InIntervalExInc(newNodeId, tempNodenodeId, tempNodesuccessor)) {
      tempNodenodeId = ClosestPrecedingFinger.run(tempNodenodeId, newNodeId,nodesObj)
      tempNodesuccessor = nodesObj(tempNodenodeId).fingerTable(0).split(",")(1).toInt
    }
    return tempNodenodeId
  }
}
