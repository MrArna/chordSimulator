package chord.algorithms

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import chord.Node.{GetFingerTable, GetIdentifier, GetSuccessor}

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
    case Calculate(id) =>
    {
      println("FP initiated")

      var nPrime = sender
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

        var cfp = context.actorOf(ClosestFingerPreceding.props(keyspace))
        var cfpFut = cfp ? ClosestFingerPreceding.Calculate(id,fingerTable)
        Await.result(cfpFut, Duration.Inf)
        nPrime = cfpFut.value.get.get.asInstanceOf[ActorRef]
      }

      sender ! nPrime
      context.stop(self)

    }
  }
}


object FindPredecessor
{

  trait Request
  case class Calculate(id: Long) extends Request

  def props(kespace: Int): Props = Props(new FindPredecessor(kespace))

}