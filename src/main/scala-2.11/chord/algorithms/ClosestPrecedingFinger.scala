package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  */
object ClosestPrecedingFinger
{

  private def InIntervalExEx(Id: Int, nid: Int, succId: Int): Boolean = {
  if (nid < succId) {
    if (Id > nid && Id < succId) { return true }
    else return false
  } else {
    if (Id > nid || Id < succId) { return true }
    else return false
  }
}


  def run(kNodeId: Int, newNodeId: Int, nodesObj: Array[Node]): Int = {
    for (i <- nodesObj(kNodeId).fingerTable.length - 1 to 0 by -1) {
      var s = nodesObj(kNodeId).fingerTable(i).split(",")
      if (InIntervalExEx(s(1).toInt, kNodeId, newNodeId)) {
        return s(1).toInt
      }
    }
    return kNodeId
  }

}
