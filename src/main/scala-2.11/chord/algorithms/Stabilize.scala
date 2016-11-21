package chord.algorithms

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import chord.Node.{GetIdentifier, GetPredecessor, GetSuccessor, SetSuccessor}
import chord.algorithms.Stabilize.Calculate

import scala.concurrent.Await

/**
  * Created by Marco on 20/11/16.
  */
class Stabilize extends Actor
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
    case Calculate(node) =>
      {


        println("-> STAB invoked")
        //n
        val nFut = node ? GetIdentifier
        Await.result(nFut,Duration.Inf)
        val n = nFut.value.get.get.asInstanceOf[Long]

        //successor
        val nodeSuccFut = node ? GetSuccessor
        Await.result(nodeSuccFut,Duration.Inf)
        val succ = nodeSuccFut.value.get.get.asInstanceOf[ActorRef]

        val succIdFut = succ ? GetIdentifier
        Await.result(succIdFut,Duration.Inf)
        val succId = succIdFut.value.get.get.asInstanceOf[Long]

        //successor.predecessor
        val nodeSuccPredFut = succ ? GetPredecessor
        Await.result(nodeSuccPredFut,Duration.Inf)
        val succPred = nodeSuccPredFut.value.get.get.asInstanceOf[ActorRef]

        //x = successor.predecessor
        val xFut = succPred ? GetIdentifier
        Await.result(xFut,Duration.Inf)
        val x = xFut.value.get.get.asInstanceOf[Long]
        if(inInterval(x,n,succId))
        {
          Await.result(node ? SetSuccessor(succPred),Duration.Inf)
        }

        val notifyAlg = context.actorOf(Notify.props())
        Await.result(succ ? Notify.Calculate(succ,node),Duration.Inf)
        println("-> STAB completed")
      }
  }
}

object Stabilize
{
  trait Request
  case class Calculate(nodeRef: ActorRef) extends Request

  trait Response
  case object StabilizationCompleted extends Response

  def props():Props = Props(new Stabilize())
}


