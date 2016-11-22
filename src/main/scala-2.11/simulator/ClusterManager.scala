package simulator


import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import chord.JumpCalculator.{Calculate, InitObserver}
import chord.Node.{DumpState, Join}
import chord.{JumpCalculator, Node}
import simulator.ClusterManager.{DumpSystem, InitMaster, NextNode}
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by Marco on 21/11/16.
  */
class ClusterManager(keySpace:Int) extends Actor
{
  var nodeIDList: List[Int] = null
  var keyspace: Int = keySpace
  var numRequests: Int = 0
  var joiningNode: List[Int] = List()
  var knownNode: ActorRef = null
  var totalNodes = Array.ofDim[ActorRef](math.pow(2, keySpace).toInt)
  var jumpCalculator:ActorRef = null

  def receive =
  {
    case InitMaster(idList: List[Int], requestNumber: Int, nextNode: List[Int]) =>
    {
      nodeIDList = idList.sorted
      numRequests = requestNumber
      joiningNode = nextNode
      var predecessor: Int = 0
      var successor: Int = 0
      jumpCalculator = context.system.actorOf(JumpCalculator.props((nodeIDList.length + joiningNode.length), numRequests))
      jumpCalculator ! InitObserver(self, joiningNode.length)
      for (index <- 0 to nodeIDList.length - 1) {
        var fingerTable = Array.ofDim[String](keySpace)
        var keys: List[Int] = List()
        totalNodes(nodeIDList(index)) = context.system.actorOf(Node.props(keyspace))
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
        totalNodes(nodeIDList(index)) ! Node.Initialize(nodeIDList(index), successor, predecessor, fingerTable, numRequests, keys, jumpCalculator)
      }
      for (i <- 0 to nodeIDList.length - 1) {
        totalNodes(nodeIDList(i)) ! Node.UpdateNetwork(totalNodes)

      }

      println("<- Initial network Built")
      println("-> Join Started")
      self ! NextNode(0)
    }

    case NextNode(nIndex:Int) =>{
      totalNodes(joiningNode(nIndex)) = context.system.actorOf(Node.props(keyspace))
      totalNodes(joiningNode(nIndex)) ! Join(joiningNode(nIndex), totalNodes(nodeIDList(0)), totalNodes, numRequests, jumpCalculator)

    }

    case DumpSystem => {


      implicit val timeout = Timeout(5 seconds)

      Await.result(jumpCalculator ? Calculate,Duration.Inf)
      for (i <- 0 until totalNodes.length) {
        if (totalNodes(i) != null) {
          totalNodes(i) ! DumpState
        }
      }
    }

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
