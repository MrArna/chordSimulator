package chord.algorithms

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import chord.Node
import chord.Node.GetFingerTable
import chord.algorithms.FixFingers.Calculate

import scala.concurrent.Await

/**
  * Created by Marco on 20/11/16.
  */
class FixFingers(keyspace: Int) extends Actor
{
  import scala.concurrent.duration._
  implicit val timeout = Timeout(60 seconds)

  override def receive: Receive =
  {
    case Calculate(node) =>
      {

        val fingerTableFut = node ? GetFingerTable
        Await.result(fingerTableFut,Duration.Inf)
        val fingerTable = fingerTableFut.value.get.get.asInstanceOf[List[(Long,ActorRef)]]


        val index = scala.util.Random.nextInt(keyspace-1) + 1
        val findSuccessorAlg = context.actorOf(FindSuccessor.props(keyspace))

        val resultFut = findSuccessorAlg ? FindSuccessor.Calculate(fingerTable(index)._1,node)
        Await.result(resultFut,Duration.Inf)
        val result = resultFut.value.get.get.asInstanceOf[ActorRef]

        Await.result(node ? Node.UpdateFingerTable(index,result),Duration.Inf)

      }
  }
}


object FixFingers
{
  trait Request
  case class Calculate(nodeRef: ActorRef) extends Request


  trait Response

  def props(keyspace: Int): Props = Props(new FixFingers(keyspace))


}