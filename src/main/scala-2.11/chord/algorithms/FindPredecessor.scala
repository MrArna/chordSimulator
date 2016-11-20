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


  private def inInterval(id: Long, lowerBound: Long, upperBound: Long): Boolean =
  {
    if (lowerBound < upperBound) {
      if (id > lowerBound && id <= upperBound) { return true }
      else return false
    } else {
      if (id > lowerBound || id <= upperBound) { return true }
      else return false
    }
  }


  override def receive: Receive =
  {
    case Calculate(id,nodeRef) =>
    {
      println("-> FP invoked")


      val cfpAlg = context.actorOf(ClosestFingerPreceding.props(keyspace.toInt))

      // n' = n
      var nPrime = nodeRef
      var nPrimeIdentifierFut = nPrime ? GetIdentifier
      Await.result(nPrimeIdentifierFut,Duration.Inf)
      var nPrimeIdentifier = nPrimeIdentifierFut.value.get.get.asInstanceOf[Long]

      //n'.successor
      var nPrimeSuccFut = nPrime ? GetSuccessor
      Await.result(nPrimeSuccFut,Duration.Inf)
      var nPrimeSucc = nPrimeSuccFut.value.get.get.asInstanceOf[ActorRef]

      var nPrimeSuccIdentifierFut = nPrimeSucc ? GetIdentifier
      Await.result(nPrimeSuccIdentifierFut,Duration.Inf)
      var nPrimeSuccIdentifier = nPrimeSuccIdentifierFut.value.get.get.asInstanceOf[Long]

      //while id not belongs to (n',n'.successor]
      while (!inInterval(id,nPrimeIdentifier,nPrimeSuccIdentifier))
      {

        //n'.finger_table
        var fingerTableFut = nPrime ? GetFingerTable
        Await.result(fingerTableFut,Duration.Inf)
        var fingerTable = fingerTableFut.value.get.get.asInstanceOf[List[(Long,ActorRef)]]


        var cfpFut = cfpAlg ? ClosestFingerPreceding.Calculate(id,nPrime)
        Await.result(cfpFut, Duration.Inf)
        nPrime = cfpFut.value.get.get.asInstanceOf[ActorRef]

        //n' = n'.closest_finger_predecessor
        nPrimeIdentifierFut = nPrime ? GetIdentifier
        Await.result(nPrimeIdentifierFut,Duration.Inf)
        nPrimeIdentifier = nPrimeIdentifierFut.value.get.get.asInstanceOf[Long]

        //updtate interval for the while condition
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