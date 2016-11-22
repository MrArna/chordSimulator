package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  *
  * The ClosestPrecedingNode algorithm is defined in the Chord paper as follows:
  *
  * {{{
  *   n.closest_preceding_node(id)
  *     for i - m downto 1
  *       if (finger[i].node IN (n, id))
  *         return finger[i].node;
  *     return n;
  * }}}
  *
  */
object ClosestPrecedingFinger
{
  // define a () interval in a ring
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
    //for i-m downto 1
    for (i <- nodesObj(kNodeId).fingerTable.length - 1 to 0 by -1) {
      //finger[i].node
      var s = nodesObj(kNodeId).fingerTable(i).split(",")
      //if (finger[i].node IN (n, id))
      if (InIntervalExEx(s(1).toInt, kNodeId, newNodeId)) {
        return s(1).toInt //return finger[i].node;
      }
    }
    return kNodeId //return n;
  }

}
