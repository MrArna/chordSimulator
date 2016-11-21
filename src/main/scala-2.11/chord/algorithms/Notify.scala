package chord.algorithms

import javax.xml.ws.Response

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.actor.Actor.Receive
import akka.util.Timeout
import chord.Node.{GetIdentifier, GetPredecessor, SetPredecessor}
import chord.algorithms.Notify.Calculate

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by Marco on 20/11/16.
  */
class Notify extends Actor
{

  import scala.concurrent.duration._
  implicit val timeout = Timeout(60 seconds)

  private def inInterval(id: Long, lowerBound: Long, upperBound:Long):Boolean =
  {
    if (lowerBound < upperBound) {
      if (id > lowerBound && id < upperBound) { return true }
      else return false
    } else {
      if (id > lowerBound || id < upperBound) { return true }
      else return false
    }
  }


  override def receive: Receive =
  {
    case Calculate(nPrime,node) =>
    {
      println("-> NOTIFY invoked")
      val nFut = node ? GetIdentifier
      Await.result(nFut,Duration.Inf)
      val n = nFut.value.get.get.asInstanceOf[Long]


      val predRefFut = node ? GetPredecessor
      Await.result(predRefFut,Duration.Inf)
      val predRef =  predRefFut.value.get.get.asInstanceOf[ActorRef]


      if (predRef == null)
      {
        Await.result(node ? SetPredecessor(nPrime),Duration.Inf)
      }
      else
      {
        val predFut = predRef ? GetIdentifier
        Await.result(predFut,Duration.Inf)
        val pred =  predFut.value.get.get.asInstanceOf[Long]

        val nPrimeIdFut = nPrime ? GetIdentifier
        Await.result(nPrimeIdFut,Duration.Inf)
        val nPrimeId =  nPrimeIdFut.value.get.get.asInstanceOf[Long]

        if(inInterval(nPrimeId,pred,n))
        {
          Await.result(node ? SetPredecessor(nPrime),Duration.Inf)
        }
      }
      println("-> NOTI completed")
    }
  }
}

object Notify
{
  trait Request
  case class Calculate(nPrime:ActorRef, nodeRef: ActorRef)

  trait Response


  def props():Props = Props(new Notify())
}