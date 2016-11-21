package simulator

import java.math.BigInteger

import akka.actor.{Actor, ActorRef, Props}
import chord.JumpCalculator.InitObserver
import chord.Node.{DumpState, Join}
import chord.{JumpCalculator, Node}
import simulator.ClusterManager.{DumpSystem, InitMaster, NextNode}

/**
  * Created by Marco on 21/11/16.
  */
class ClusterManager(keySpace:Int) extends Actor
{
  var nodeIDList: List[Int] = null
  var keyspace: Int = keySpace
  var numRequests: Int = 0
  var key: Int = 0
  var joiningNode: List[Int] = List()
  var knownNode: ActorRef = null
  var peerNodes = Array.ofDim[ActorRef](math.pow(2, keySpace).toInt)
  var hopActor:ActorRef = null

  def receive =
  {
    case InitMaster(idList: List[Int], requestNumber: Int, nextNode: List[Int]) =>
    {
      nodeIDList = idList.sorted
      numRequests = requestNumber
      joiningNode = nextNode
      var predecessor: Int = 0
      var successor: Int = 0
      hopActor = context.system.actorOf(JumpCalculator.props((nodeIDList.length + joiningNode.length), numRequests))
      hopActor ! InitObserver(self, joiningNode.length)
      for (index <- 0 to nodeIDList.length - 1) {
        var fingerTable = Array.ofDim[String](keySpace)
        var keys: List[Int] = List()
        peerNodes(nodeIDList(index)) = context.system.actorOf(Node.props(keyspace))
        //set Successor and predecessor for initial Network
        if (index == 0) {
          predecessor = nodeIDList(nodeIDList.length - 1)
          successor = nodeIDList(index + 1)
        } else if (index == nodeIDList.length - 1) {
          predecessor = nodeIDList(index - 1)
          successor = nodeIDList(0)
        } else {
          predecessor = nodeIDList(index - 1)
          successor = nodeIDList(index + 1)
        }

        //set FingerTable for initial Network
        for (fingerindex <- 0 to keySpace - 1) {

          var start = (nodeIDList(index) + math.pow(2, fingerindex).toInt) % math.pow(2, keySpace).toInt
          var fingerNode: Int = 0
          if (nodeIDList.contains(start)) {
            fingerNode = start
          } else if (start > nodeIDList(nodeIDList.length - 1)) {
            fingerNode = nodeIDList(0)
          } else if (start < nodeIDList(0)) {
            fingerNode = nodeIDList(0)
          } else {
            var j = 0
            while (j < (nodeIDList.length - 1)) {
              if (start > nodeIDList(j) && start < nodeIDList(j + 1)) {
                fingerNode = nodeIDList(j + 1)
              }
              j += 1

            }

          }

          fingerTable(fingerindex) = (start + "," + fingerNode)
        }

        if (index == 0) {
          var x = nodeIDList(nodeIDList.length - 1) + 1
          keys = List()
          for (i <- x to math.pow(2, keySpace).toInt - 1) {
            keys ::= i
          }
          for (i <- 0 to nodeIDList(index)) {
            keys ::= i
          }
        } else if (index != 0 && index <= nodeIDList.length - 1) {
          keys = List()
          for (i <- nodeIDList(index - 1) + 1 to nodeIDList(index)) {
            keys ::= i
          }
        }
        //println(index)
        peerNodes(nodeIDList(index)) ! Node.Initialize(nodeIDList(index), successor, predecessor, fingerTable, numRequests, keys, hopActor)
      }
      for (i <- 0 to nodeIDList.length - 1) {
        peerNodes(nodeIDList(i)) ! Node.UpdateNetwork(peerNodes)

      }

      println("Initial network Built..")
      println("Join Started..")
      self ! NextNode(0)
    }

    case NextNode(nIndex:Int) =>{
      peerNodes(joiningNode(nIndex)) = context.system.actorOf(Node.props(keyspace))
      peerNodes(joiningNode(nIndex)) ! Join(joiningNode(nIndex), peerNodes(nodeIDList(0)), peerNodes, numRequests, hopActor)

    }

    case DumpSystem => {
      for (i <- 0 until peerNodes.length) {
        if (peerNodes(i) != null) {
          peerNodes(i) ! DumpState
        }
      }
    }

  }

  def getKey(): Int = {
    val keyString = "Marco" + "Arnaboldi" + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256)
    val md = java.security.MessageDigest.getInstance("SHA-1")
    var encodedString = md.digest(keyString.getBytes("UTF-8")).map("%02x".format(_)).mkString
    encodedString = new BigInteger(encodedString, 16).toString(2)
    var keyHash = Integer.parseInt(encodedString.substring(encodedString.length() - keySpace), 2)
    if (nodeIDList.contains(keyHash)) {
      keyHash = getKey()
    }
    return keyHash
  }

}


object ClusterManager
{
  trait Request
  case class InitMaster(nodeIDList: List[Int], numRequests: Int, joiningNode: List[Int]) extends Request
  case class NextNode(nodeIndex:Int) extends Request
  case object DumpSystem extends Request

  trait Response


  def props(keyspace: Int):Props = Props(new ClusterManager(keyspace))
}
