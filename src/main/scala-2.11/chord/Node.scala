package chord

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import chord.algorithms.{ClosestFingerPreceding, FindPredecessor, FindSuccessor}
import chord.algorithms.ClosestFingerPreceding.Calculate

import scala.concurrent.Await
import scala.concurrent.duration.Duration


/**
  * Created by Marco on 16/11/16.
  */
class Node(id: Long, keyspace: Long) extends Actor with ActorLogging {

  var identifier = id

  var pred = self
  var succ = self

  var fingerTable: List[(Long, ActorRef)] = List.empty

  import Node._

  private val idModulus = Math.pow(2.toDouble,keyspace)


  import scala.concurrent.duration._
  implicit val timeout = Timeout(60 seconds)


  override def preStart(): Unit = {
    super.preStart()
    if (fingerTable.isEmpty) {
      for (k <- 0 until keyspace.toInt) {
        fingerTable ::= ((identifier + Math.pow(2.toDouble, k.toDouble).toLong) % (idModulus.toInt), self)
      }
      //println(fingerTable)
    }
  }


  private def findSuccessor(id: Long)=
  {
    val fsAlg = context.actorOf(FindSuccessor.props(keyspace.toInt))
    fsAlg ! FindSuccessor.Calculate(id)
  }

  private def findPredecessor(id: Long) =
  {
    val fpAlg = context.actorOf(FindPredecessor.props(keyspace.toInt))
    fpAlg ! FindPredecessor.Calculate(id)
  }

  private def closestFingerPreceding(id: Long) =
  {
    val cfpAlg = context.actorOf(ClosestFingerPreceding.props(keyspace.toInt))
    cfpAlg ! Calculate(id, fingerTable)
  }


  private def initFingerTable(nPrime: ActorRef) =
  {

   /* var nPrimeFindSuccFut = nPrime ? FindSuccessor(fingerTable(0)._1)
    Await.result(nPrimeFindSuccFut,Duration.Inf)
    var nPrimeFindSucc = nPrimeFindSuccFut.value.get.get.asInstanceOf[ActorRef]

    println("InitFinger table 1")

    fingerTable = fingerTable.updated(0,fingerTable(0).copy(_2 = nPrimeFindSucc))

    succ = fingerTable(0)._2

    val succPredFut = succ ? GetPredecessor
    Await.result(succPredFut,Duration.Inf)
    var succPred = succPredFut.value.get.get.asInstanceOf[ActorRef]

    pred = succPred

    Await.result(succ ? SetPredecessor(self),Duration.Inf)

    for(i <- 0 until (keyspace-1).toInt)
    {
      var identifierFut = fingerTable(i)._2 ? GetIdentifier
      Await.result(identifierFut,Duration.Inf)
      var node = identifierFut.value.get.get.asInstanceOf[Long]

      if(fingerTable(i+1)._1 >= identifier && fingerTable(i+1)._1 < node)
      {
        fingerTable = fingerTable.updated(i+1, fingerTable(i+1).copy(_2 = fingerTable(i)._2))
      }
      else
      {
        nPrimeFindSuccFut = nPrime ? FindSuccessor(fingerTable(i+1)._1)
        Await.result(nPrimeFindSuccFut,Duration.Inf)
        nPrimeFindSucc = nPrimeFindSuccFut.value.get.get.asInstanceOf[ActorRef]
        fingerTable = fingerTable.updated(i+1,fingerTable(0).copy(_2 = nPrimeFindSucc))

      }


    }

    println("Finger table -> " + fingerTable)
    sender ! DumpState*/

  }




  override def receive: Receive =
  {
    case AssignKey(key) =>


    case LetJoin(newNode) => println("Node " + this + " is letting join") ;newNode ! CmonJoin(self)


    case CmonJoin(existingNodeRef) => println("Node " + this + " is joining") ;initFingerTable(existingNodeRef)

    case DumpState => println(this)

    case GetPredecessor => sender ! pred
    case GetSuccessor => sender ! succ
    case GetIdentifier => sender ! identifier
    case GetFingerTable => sender ! fingerTable

    case TestFindSuccessor(id) => findSuccessor(id)



  }


  override def toString():String =
  {
    "xx" + identifier.toString + "xx"
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
  case class SetPredecessor(pred: ActorRef) extends Request
  case class TestFindSuccessor(id:Long) extends Request

  trait Response
  case class CmonJoin(existingNodeRef: ActorRef) extends Response


  def props(nodeId: Long, keyspace: Long): Props = Props(new Node(nodeId,keyspace))
}
