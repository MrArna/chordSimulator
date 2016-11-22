package chord.algorithms

import chord.Node

/**
  * Created by Marco on 21/11/16.
  *
  *
  * The InitFingerTable algorithm is defined in the Chord paper as follows:
  *
  *
  *
  * n.init_finger_table(n')
  *   finger[1].node = n'.find_successor(finger[1].start)
  *   predecessor = successor.predecessor
  *   successor.predecessor = n
  *   for i=1 to m-1
  *     if (finger[i+1].start belongs [n,finger[i].node))
  *       finger[i+1].node = finger[i].node
  *     else
  *        finger[i+1].node = n'.find_successor(finger[i].start)
  *
  *
  */
object InitFingerTable
{
  //defines [) interval
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
    //successor
    node.succ = FindSuccessor.run(kNodeObj.identifier, node.fingerTable(0).split(",")(0).toInt,node.nodesObj)

    //finger[1].node = n'.find_successor(finger[1].start)
    node.fingerTable(0) = node.fingerTable(0).split(",")(0) + "," + FindSuccessor.run(kNodeObj.identifier, node.fingerTable(0).split(",")(0).toInt,node.nodesObj)

    //predecessor = successor.predecessor
    node.pred = node.nodesObj(node.succ).pred

    //successor.predecessor = n
    node.nodesObj(node.succ).pred = node.identifier

    //for i=1 to m-1
    for (i <- 0 to node.fingerTable.length - 2) {
      //if (finger[i+1].start belongs [n,finger[i].node))
      if (InIntervalIncEx(node.fingerTable(i + 1).split(",")(0).toInt, node.identifier, node.fingerTable(i).split(",")(1).toInt)) {
        //finger[i+1].node = finger[i].node
        node.fingerTable(i + 1) = node.fingerTable(i + 1).split(",")(0) + "," + node.fingerTable(i).split(",")(1)
      } else {
        //finger[i+1].node = n'.find_successor(finger[i].start)
        node.fingerTable(i + 1) = node.fingerTable(i + 1).split(",")(0) + "," + FindSuccessor.run(kNodeObj.identifier, node.fingerTable(i + 1).split(",")(0).toInt,node.nodesObj)
      }

    }
    //predecessor.successor = n
    node.nodesObj(node.pred).succ = node.identifier
  }
}
