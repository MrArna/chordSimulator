package chord

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import akka.util.Timeout
import chord.algorithms.ClosestFingerPreceding.Calculate
import chord.algorithms.Join.JoinCompleted
import chord.algorithms._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


/**
  * Created by Marco on 16/11/16.
  */
class Node(id: Long, keyspace: Long, clusterRef: ActorRef) extends Actor with ActorLogging {

  var identifier = id

  var pred = self
  var succ = self

  val stabilize = context.actorOf(Stabilize.props())
  val fixFinger = context.actorOf(FixFingers.props(keyspace.toInt))

  var scheduledStabilize: Cancellable = null
  var schduledFixfinger: Cancellable = null


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
    cfpAlg ! Calculate(id,self)
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
    case GetSuccessor => sender ! fingerTable(0)._2
    case GetIdentifier => sender ! identifier
    case GetFingerTable => sender ! fingerTable
    case SetSuccessor(s) => {succ = s; fingerTable = fingerTable.updated(0,fingerTable(0).copy(_2 = s)) ; sender ! SuccessorSettled}
    case SetFingertable(ft) => {fingerTable = ft; succ = ft(0)._2; sender ! FingerTableSettled}

    case SetPredecessor(p) =>
    {
      pred = p
      sender ! PredecessorSettled
    }

    case UpdateFingerTable(i,node) =>
    {
      fingerTable = fingerTable.updated(i,fingerTable(0).copy(_2 = node))
    }

    case TestFindSuccessor(id) => findSuccessor(id)

    case JoinCompleted(id) =>
    {
      println(identifier)
      scheduledStabilize = context.system.scheduler.schedule(
        Duration.create(10, TimeUnit.MILLISECONDS),
        Duration.create(50, TimeUnit.MILLISECONDS),
        stabilize,
        Stabilize.Calculate(self)
      )

      schduledFixfinger =context.system.scheduler.schedule(
        Duration.create(10, TimeUnit.MILLISECONDS),
        Duration.create(50, TimeUnit.MILLISECONDS),
        fixFinger,
        FixFingers.Calculate(self)
      )
    }



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
  case class SetFingertable(fingerTable: List[(Long,ActorRef)]) extends Request
  case class UpdateFingerTable(i: Int,node: ActorRef) extends Request

  trait Response
  case class CmonJoin(existingNodeRef: ActorRef) extends Response
  case object FingerTableSettled extends Response
  case object SuccessorSettled extends Response
  case object PredecessorSettled extends Response

  def props(nodeId: Long, keyspace: Long, clusterRef: ActorRef): Props = Props(new Node(nodeId,keyspace,clusterRef))
}
