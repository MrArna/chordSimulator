package chord.algorithms

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import chord.Node._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by Marco on 17/11/16.
  */
class FindPredecessor(keyspace: Int) extends Actor
{
  import FindPredecessor._
  import scala.concurrent.duration._
  implicit val timeout = Timeout(60 seconds)

  override def receive: Receive =
  {
    case Calculate(id,nodeRef) =>
    {
      println("-> FP invoked")

      var nPrime = nodeRef
      var nPrimeIdentifierFut = nPrime ? GetIdentifier
      Await.result(nPrimeIdentifierFut,Duration.Inf)
      var nPrimeIdentifier = nPrimeIdentifierFut.value.get.get.asInstanceOf[Long]

      var nPrimeSuccFut = nPrime ? GetSuccessor
      Await.result(nPrimeSuccFut,Duration.Inf)
      var nPrimeSucc = nPrimeSuccFut.value.get.get.asInstanceOf[ActorRef]

      var nPrimeSuccIdentifierFut = nPrimeSucc ? GetIdentifier
      Await.result(nPrimeSuccIdentifierFut,Duration.Inf)
      var nPrimeSuccIdentifier = nPrimeSuccIdentifierFut.value.get.get.asInstanceOf[Long]

      while (id <= nPrimeIdentifier && id > nPrimeSuccIdentifier)
      {

        var fingerTableFut = nPrime ? GetFingerTable
        Await.result(fingerTableFut,Duration.Inf)
        var fingerTable = fingerTableFut.value.get.get.asInstanceOf[List[(Long,ActorRef)]]

        val cfpAlg = context.actorOf(ClosestFingerPreceding.props(keyspace.toInt))
        var cfpFut = cfpAlg ? ClosestFingerPreceding.Calculate(id,fingerTable,nodeRef)
        Await.result(cfpFut, Duration.Inf)
        nPrime = cfpFut.value.get.get.asInstanceOf[ActorRef]

        nPrimeIdentifierFut = nPrime ? GetIdentifier
        Await.result(nPrimeIdentifierFut,Duration.Inf)
        nPrimeIdentifier = nPrimeIdentifierFut.value.get.get.asInstanceOf[Long]

        nPrimeSuccFut = nPrime ? GetSuccessor
        Await.result(nPrimeSuccFut,Duration.Inf)
        nPrimeSucc = nPrimeSuccFut.value.get.get.asInstanceOf[ActorRef]

        nPrimeSuccIdentifierFut = nPrimeSucc ? GetIdentifier
        Await.result(nPrimeSuccIdentifierFut,Duration.Inf)
        nPrimeSuccIdentifier = nPrimeSuccIdentifierFut.value.get.get.asInstanceOf[Long]

      }

      sender ! nPrime
      //context.stop(self)

    }
  }
}


object FindPredecessor
{

  trait Request
  case class Calculate(id: Long, nodeRef: ActorRef) extends Request

  def props(kespace: Int): Props = Props(new FindPredecessor(kespace))

}