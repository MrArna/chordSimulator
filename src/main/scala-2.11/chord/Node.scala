package chord

import java.util.Calendar
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
  //used for keys lookup during request
  var fingerTableStart = Array.ofDim[Int](keySpace)
  var fingerTableNode = Array.ofDim[Int](keySpace)
  var nodes: Array[ActorRef] = null
  var jumpCalculator: ActorRef = null
  var knownNode: ActorRef = null
  var nodeSpace: Int = math.pow(2, keySpace).toInt
  var knownNodeObj: Node = null
  var nodesObj: Array[Node] = Array.ofDim[Node](nodeSpace)

  def receive = {

    //initialize node
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

    //update the nodes
    case UpdateNetwork(allNodes: Array[ActorRef]) => {
      nodes = allNodes
    }

    /**
      *
      * Join is defined in the paper as:
      *
      * n.join(n')
      *   if(n')
      *     init_finger_table(n')
      *     update_others()
      *     //update key
      *   else
      *     //init the first node
      *
      * in this implementationn only the first branch is implemented since the network is built offline,
      * so there is always a known node. Furhtermo more the branch is splitted in 3 phases
      *
      */

    //pahse 0 initialization
    case Join(joiningId: Int, kNode: ActorRef, allNodes: Array[ActorRef], requestNumber: Int, jumpCalc: ActorRef) => {
      identifier = joiningId
      knownNode = kNode
      nodes = allNodes
      numRequests = requestNumber
      jumpCalculator = jumpCalc
      knownNode ! GetKnownInstance
      //create the finger table structure
      for (i <- 0 to keySpace - 1) {
        var start = (identifier + math.pow(2, i).toInt) % math.pow(2, keySpace).toInt
        fingerTable(i) = (start + ",X")
      }
      //get the nodes instances and populate nodes array
      for (i <- 0 to nodes.length - 1) {
        if (nodes(i) != null) {
          nodes(i) ! GetInstance
        }

      }
      //launch the real join
      context.system.scheduler.scheduleOnce(FiniteDuration(3000, TimeUnit.MILLISECONDS), self, InitJoin)

    }
      // phase 1: the real join begins
    case InitJoin => {
      initFingerTable(knownNodeObj)
      updateOthersFingerTable()

      context.system.scheduler.scheduleOnce(FiniteDuration(3000, TimeUnit.MILLISECONDS), self, UpdateKeys)

    }

      //pahse 2: the update keys begins
    case UpdateKeys => {

      var presentNodes: List[Int] = List()
      //look for nodes
      for (i <- 0 to nodes.length - 1) {
        if (nodes(i) != null) {
          presentNodes ::= nodesObj(i).identifier
        }

      }
      presentNodes = presentNodes.sorted
      var newKeys: List[Int] = List()
      //for each node assign the right keys to it
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
        nodes(presentNodes(index)) ! SetKeys(newKeys)
      }
      println("Node "+identifier+" Joined")

      //notify that one join is completed
      jumpCalculator ! JoinObserver(presentNodes, 1)


    }
    // if all jon completed querying can start
    case JoinCompleted(currentNodes: List[Int]) =>
    {
      println()
      println("-> Request Processing Started")
      for (i <- 0 to currentNodes.length - 1) {
        nodes(currentNodes(i)) ! StartQuerying
      }

    }
    //define query repetition
    case StartQuerying => {
      requestRepetition = context.system.scheduler.schedule(FiniteDuration(5000, TimeUnit.MILLISECONDS), FiniteDuration(60*1000/numRequests, TimeUnit.MILLISECONDS), self, Start)

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

      //Lookup in order to find the requested key
    case LookupFingerTable(keyValue: Int, orgNode: Int, jump: Int) => {
      var key = keyValue
      requestFrom = orgNode
      jumpCount = jump + 1
      jumpCalculator ! JumpDone

      if (allKeys.contains(key)) {
        nodes(orgNode) ! Completed
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
    //create a query for the current node
    case Start => {
      val newKey = scala.util.Random.nextInt(math.pow(2, keyspace).toInt)
      self ! LookupFingerTable(newKey, identifier, -1)

    }

    case Completed => {
      jumpCalculator ! RequestCompleted
    }

      //dump the node state
    case DumpState =>
    {
      import java.io._
      val log = new File("log.txt")

      val pw: PrintWriter = null

      var ftStr = ""
      for(i <- 0 until fingerTable.length-1)
      {
        ftStr = ftStr +  "(" + fingerTable(i) + ") "
      }
      ftStr = ftStr +  "(" +fingerTable(fingerTable.length-1) + ")"


      if(log.exists())
      {
        val pw = new PrintWriter(new FileOutputStream(log, true))
        pw.append(
          "{\t\"node\":\"" + identifier + "\",\n" +
            " \t\"fingerTable\": [" + ftStr + "],\n " +
            "\t\"keys\": \"" + allKeys + "\"\n}\n")
        pw.close
      }
      else
      {
        val pw = new PrintWriter(log)
        pw.append(
          "{\t\"node\":\"" + identifier + "\",\n" +
            " \t\"fingerTable\": [" + ftStr + "],\n " +
            "\t\"keys\": \"" + allKeys + "\"\n}\n")
        pw.close
      }


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
  case object Completed extends Response
  case class JoinCompleted(currentNodes:List[Int]) extends Response


  def props(keyspace: Int): Props = Props(new Node(keyspace))
}
