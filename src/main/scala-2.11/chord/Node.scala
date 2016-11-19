package chord

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import chord.algorithms.ClosestFingerPreceding.Calculate
import chord.algorithms._


/**
  * Created by Marco on 16/11/16.
  */
class Node(id: Long, keyspace: Long, clusterRef: ActorRef) extends Actor with ActorLogging {

  var identifier = id

  var pred = self
  var succ = self

  var fingerTable: List[(Long, ActorRef)] = List.empty

  import Node._

  private val idModulus = Math.pow(2.toDouble,keyspace)


  import scala.concurrent.duration._
  implicit val timeout = Timeout(60 seconds)


  private def findSuccessor(id: Long)=
  {
    val fsAlg = context.actorOf(FindSuccessor.props(keyspace.toInt))
    fsAlg ! FindSuccessor.Calculate(id,self)
  }

  private def findPredecessor(id: Long) =
  {
    val fpAlg = context.actorOf(FindPredecessor.props(keyspace.toInt))
    fpAlg ! FindPredecessor.Calculate(id,self)
  }

  private def closestFingerPreceding(id: Long) =
  {
    val cfpAlg = context.actorOf(ClosestFingerPreceding.props(keyspace.toInt))
    cfpAlg ! Calculate(id, fingerTable,self)
  }


  private def join(nPrime: ActorRef) =
  {
    val jAlg = context.actorOf(Join.props(keyspace.toInt,clusterRef))
    jAlg ! Join.Calculate(nPrime,self)
  }




  override def receive: Receive =
  {
    case AssignKey(key) =>


    case LetJoin(newNode) =>
      {
        println("Node " + this + " is letting join")
        if(newNode == self)
        {
          newNode ! CmonJoin(null)
        }
        else
        {
          newNode ! CmonJoin(self)
        }
      }


    case CmonJoin(existingNodeRef) => println("Node " + this + " is joining") ; join(existingNodeRef)

    case DumpState => println(this.toString())

    case GetPredecessor => sender ! pred
    case GetSuccessor => sender ! succ
    case GetIdentifier => sender ! identifier
    case GetFingerTable => sender ! fingerTable
    case SetSuccessor(s) => succ = s
    case SetFingertable(ft) => fingerTable = ft; succ = ft(0)._2
    case SetPredecessor(p) => pred = p

    case TestFindSuccessor(id) => findSuccessor(id)

    case InvokeClosesFingerPreceding(id) => closestFingerPreceding(id)
    case InvokeFindPredecessor(id) => findPredecessor(id)
    case InvokeFindSuccessor(id) => findSuccessor(id)

  }


  override def toString():String =
  {
    "[id: " + identifier + ", ref: " + self + ", fingerTable: " + fingerTable + "]"
  }


}


object Node
{


  trait Request
  case class AssignKey(key: Int) extends Request
  case class LetJoin(newNodeRef: ActorRef) extends Request
  case object DumpState extends Request
  case object GetPredecessor extends Request
  case object GetSuccessor extends Request
  case object GetIdentifier extends Request
  case object GetFingerTable extends Request
  case class InvokeFindPredecessor(id: Long) extends Request
  case class InvokeClosesFingerPreceding(id: Long) extends Request
  case class InvokeFindSuccessor(id: Long) extends Request
  case class SetPredecessor(pred: ActorRef) extends Request
  case class TestFindSuccessor(id:Long) extends Request
  case class SetSuccessor(succ: ActorRef) extends Request
  case class SetFingertable(fingerTable: List[(Long,ActorRef)])

  trait Response
  case class CmonJoin(existingNodeRef: ActorRef) extends Response


  def props(nodeId: Long, keyspace: Long, clusterRef: ActorRef): Props = Props(new Node(nodeId,keyspace,clusterRef))
}
