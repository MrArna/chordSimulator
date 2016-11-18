package chord.algorithms

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import chord.Node.GetSuccessor
import chord.algorithms.FindSuccessor.Calculate

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by Marco on 17/11/16.
  */
class FindSuccessor(keyspace: Int) extends Actor
{
  import scala.concurrent.duration._
  implicit val timeout = Timeout(60 seconds)


  override def receive: Receive =
  {
    case Calculate(id) =>
      {
        val fpAlg = context.actorOf(FindPredecessor.props(keyspace))
        val fpAlgFut = fpAlg ? FindPredecessor.Calculate(id)
        Await.ready(fpAlgFut,Duration.Inf)
        val nPrime = fpAlgFut.value.get.get.asInstanceOf[ActorRef]

        val nPrimeSuccFut = nPrime ? GetSuccessor
        Await.result(nPrimeSuccFut,Duration.Inf)
        sender ! nPrimeSuccFut.value.get.get.asInstanceOf[ActorRef]
        context.stop(self)
      }
  }
}


object FindSuccessor
{
  trait Request
  case class Calculate(id: Long) extends Request


  def props(keyspace: Int):Props  = Props(new FindSuccessor(keyspace))



}