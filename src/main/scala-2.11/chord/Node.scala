package chord

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import chord.JumpCalculator.{Calculate, JoinObserver, JumpDone, RequestCompleted}
import chord.Node._
import chord.algorithms._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration


/**
  * Created by Marco on 16/11/16.
  */
class Node(keySpace: Int) extends Actor with ActorLogging {

  var identifier: Int = 0
  var succ: Int = 0
  var pred: Int = 0
  var fingerTable = Array.ofDim[String](keySpace)
  var keyspace: Int = keySpace
  var allKeys: List[Int] = List()
  var requestFrom: Int = 0
  var jumpCount: Int = 0
  var requestRepetition: Cancellable = null
  var numRequests: Int = 0
  var requestCompleteCount: Int = 0
  var fingerTableStart = Array.ofDim[Int](keySpace)
  var fingerTableNode = Array.ofDim[Int](keySpace)
  var nodes: Array[ActorRef] = null
  var jumpCalculator: ActorRef = null
  var knownNode: ActorRef = null
  var nodeSpace: Int = math.pow(2, keySpace).toInt
  var knownNodeObj: Node = null
  var nodesObj: Array[Node] = Array.ofDim[Node](nodeSpace)

  def receive = {

    case Initialize(nid: Int, succ: Int, pred: Int, lookup: Array[String], requestNumber: Int, totalKeys: List[Int], jActor: ActorRef) => {
      identifier = nid
      this.succ = succ
      this.pred = pred
      fingerTable = lookup
      numRequests = requestNumber
      allKeys = totalKeys
      jumpCalculator = jActor
      for (i <- 0 to keyspace - 1) {
        fingerTableStart(i) = fingerTable(i).split(",")(0).toInt
        fingerTableNode(i) = fingerTable(i).split(",")(1).toInt
      }
    }

    case UpdateNetwork(allNodes: Array[ActorRef]) => {
      nodes = allNodes
    }

    case Join(joiningId: Int, kNode: ActorRef, allNodes: Array[ActorRef], requestNumber: Int, jumpCalc: ActorRef) => {
      identifier = joiningId
      knownNode = kNode
      nodes = allNodes
      numRequests = requestNumber
      jumpCalculator = jumpCalc
      knownNode ! GetKnownInstance
      for (i <- 0 to keySpace - 1) {
        var start = (identifier + math.pow(2, i).toInt) % math.pow(2, keySpace).toInt
        fingerTable(i) = (start + ",X")
      }

      for (i <- 0 to nodes.length - 1) {
        if (nodes(i) != null) {
          nodes(i) ! GetInstance
        }

      }
      context.system.scheduler.scheduleOnce(FiniteDuration(3000, TimeUnit.MILLISECONDS), self, InitJoin)

    }

    case InitJoin => {
      initFingerTable(knownNodeObj)
      updateOthersFingerTable()

      context.system.scheduler.scheduleOnce(FiniteDuration(3000, TimeUnit.MILLISECONDS), self, UpdateKeys)

    }
    case UpdateKeys => {

      var presentNodes: List[Int] = List()
      for (i <- 0 to nodes.length - 1) {
        if (nodes(i) != null) {
          presentNodes ::= nodesObj(i).identifier
        }

      }
      presentNodes = presentNodes.sorted
      var newKeys: List[Int] = List()
      for (index <- 0 to presentNodes.length - 1) {
        if (index == 0) {
          var x = presentNodes(presentNodes.length - 1) + 1
          newKeys = List()
          for (i <- x to math.pow(2, keySpace).toInt - 1) {
            newKeys ::= i
          }
          for (i <- 0 to presentNodes(index)) {
            newKeys ::= i
          }
        } else if (index != 0 && index <= presentNodes.length - 1) {
          newKeys = List()
          for (i <- presentNodes(index - 1) + 1 to presentNodes(index)) {
            newKeys ::= i
          }
        }
        //println(newKeys.toList)
        nodes(presentNodes(index)) ! SetKeys(newKeys)
      }
      println("Node "+identifier+" Joined..")


      jumpCalculator ! JoinObserver(presentNodes, 1)


    }

    case JoinCompleted(currentNodes: List[Int]) =>
    {
      println()
      println("Request Processing Started...")
      for (i <- 0 to currentNodes.length - 1) {
        nodes(currentNodes(i)) ! StartQuerying
      }

    }

    case StartQuerying => {
      requestRepetition = context.system.scheduler.schedule(FiniteDuration(5000, TimeUnit.MILLISECONDS), FiniteDuration(100, TimeUnit.MILLISECONDS), self, Start)

    }

    case GetInstance => {
      sender ! SetInstance(this, identifier)

    }
    case SetInstance(rObj: Node, id: Int) => {
      nodesObj(id) = rObj
    }
    case GetKnownInstance => {
      sender ! SetKnownInstance(this)

    }
    case SetKnownInstance(rObj: Node) => {
      knownNodeObj = rObj
    }

    case SetKeys(nKeys: List[Int]) => {
      allKeys = nKeys
    }

    case UpdateFingerTable(newNodes: List[Int], newValue: Int) => {
      UpdateFingerTableAlg.run(newNodes,newValue,this)

    }

    case LookupFingerTable(keyValue: Int, orgNode: Int, jump: Int) => {
      var key = keyValue
      requestFrom = orgNode
      jumpCount = jump + 1
      jumpCalculator ! JumpDone

      if (allKeys.contains(key)) {
        nodes(orgNode) ! Completed(jumpCount)
      } else if (fingerTableStart.contains(key)) {
        nodes(fingerTableNode(fingerTableStart.indexOf(key))) ! LookupFingerTable(key, requestFrom, jumpCount)
      } else {
        if (inIntervalExEx(key, fingerTableStart(keySpace - 1), fingerTableStart(0))) {
          nodes(fingerTableNode(keySpace - 1)) ! LookupFingerTable(key, requestFrom, jumpCount)
        } else {
          for (i <- 0 to keySpace - 2) {
            if (inIntervalExEx(key, fingerTableStart(i), fingerTableStart(i + 1))) {
              nodes(fingerTableNode(i)) ! LookupFingerTable(key, requestFrom, jumpCount)
            }

          }

        }

      }

    }

    case Start => {
      val newKey = scala.util.Random.nextInt(math.pow(2, keyspace).toInt)
      self ! LookupFingerTable(newKey, identifier, -1)

    }

    case Completed(hopCount: Int) => {
      jumpCalculator ! RequestCompleted
    }

    case DumpState =>
    {

      var ftStr = ""
      for(i <- 0 until fingerTable.length-1)
      {
        ftStr = ftStr +  "(" + fingerTable(i) + ") "
      }
      ftStr = ftStr +  "(" +fingerTable(fingerTable.length-1) + ")"

      println(
        "{\"node\":\"" + identifier + "\",\n" +
        " \"fingerTable\": [" + ftStr + "],\n " +
          "\"keys\": \"" + allKeys + "\"}")
    }

  }
  def inIntervalExEx(Id: Int, first: Int, second: Int): Boolean = {
    if (first < second) {
      if (Id > first && Id < second) { return true }
      else return false
    } else {
      if (Id > first || Id < second) { return true }
      else return false
    }
  }

  def initFingerTable(kNodeObj: Node) = {

    InitFingerTable.run(kNodeObj,this)

  }

  def updateOthersFingerTable() = {
    UpdateOthersFingerTable.run(this)
  }

  def findSuccessor(kNodeId: Int, newNodeId: Int): Int = {


    return FindSuccessor.run(kNodeId,newNodeId,nodesObj)
  }

  def findPredecessor(kNodeId: Int, newNodeId: Int): Int = {


    return FindPredecessor.run(kNodeId,newNodeId,nodesObj)
  }

  def closestPrecedingFinger(kNodeId: Int, newNodeId: Int): Int = {

    return ClosestPrecedingFinger.run(kNodeId,newNodeId,nodesObj)

  }

}


object Node
{


  trait Request
  case class Initialize(nodeID: Int, successor: Int, predecssor: Int, fingerTable: Array[String], numRequests: Int, allKeys: List[Int], hopActor: ActorRef) extends Request
  case class LookupFingerTable(key: Int, requestFrom: Int, hopCount: Int) extends Request
  case object Start extends Request
  case class UpdateNetwork(networkNodes: Array[ActorRef]) extends Request
  case object InitJoin extends Request
  case class Join(joiningNodeId: Int, knownNode: ActorRef, networkNodes: Array[ActorRef], numRequests: Int, hopActor: ActorRef) extends Request
  case object GetInstance extends Request
  case class SetInstance(receivedObj: Node, nodeId: Int) extends Request
  case object GetKnownInstance extends Request
  case class SetKnownInstance(receivedObj: Node) extends Request
  case class UpdateFingerTable(affectedNodes: List[Int], updatedValue: Int) extends Request
  case object StartQuerying extends Request
  case object UpdateKeys extends Request
  case class SetKeys(newKeys: List[Int]) extends Request
  case object DumpState extends Request

  trait Response
  case class Completed(totalHops: Int) extends Response
  case class JoinCompleted(currentNodes:List[Int]) extends Response


  def props(keyspace: Int): Props = Props(new Node(keyspace))
}
