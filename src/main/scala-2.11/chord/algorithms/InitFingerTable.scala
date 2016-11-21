package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  */
object InitFingerTable
{

  private def InIntervalIncEx(Id: Int, nid: Int, succId: Int): Boolean = {
    if (nid < succId) {
      if (Id >= nid && Id < succId) { return true }
      else return false
    } else {
      if (Id >= nid || Id < succId) { return true }
      else return false
    }
  }


  def run(kNodeObj: Node, node: Node) =
  {
    node.succ = FindSuccessor.run(kNodeObj.identifier, node.fingerTable(0).split(",")(0).toInt,node.nodesObj)
    node.fingerTable(0) = node.fingerTable(0).split(",")(0) + "," + FindSuccessor.run(kNodeObj.identifier, node.fingerTable(0).split(",")(0).toInt,node.nodesObj)
    node.pred = node.nodesObj(node.succ).pred
    node.nodesObj(node.succ).pred = node.identifier
    for (i <- 0 to node.fingerTable.length - 2) {
      if (InIntervalIncEx(node.fingerTable(i + 1).split(",")(0).toInt, node.identifier, node.fingerTable(i).split(",")(1).toInt)) {
        node.fingerTable(i + 1) = node.fingerTable(i + 1).split(",")(0) + "," + node.fingerTable(i).split(",")(1)
      } else {

        node.fingerTable(i + 1) = node.fingerTable(i + 1).split(",")(0) + "," + FindSuccessor.run(kNodeObj.identifier, node.fingerTable(i + 1).split(",")(0).toInt,node.nodesObj)
      }

    }
    node.nodesObj(node.pred).succ = node.identifier
  }
}
