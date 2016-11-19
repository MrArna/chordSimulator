package chord.algorithms

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import chord.Node.GetIdentifier
import chord.algorithms.ClosestFingerPreceding.Calculate

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by Marco on 17/11/16.
  */
class ClosestFingerPreceding(keyspace: Int) extends Actor
{

  import scala.concurrent.duration._
  implicit val timeout = Timeout(60 seconds)



  private def inInterval(id: Long, lowerBound: Long, upperBound: Long): Boolean =
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
    case Calculate(id,fingerTable,nodeRef) =>
    {
      println("-> CFP invoked")
      var identifierFut = nodeRef  ? GetIdentifier
      Await.result(identifierFut,Duration.Inf)
      var identifier = identifierFut.value.get.get.asInstanceOf[Long]
      for(i <- (keyspace-1) to 0 by -1)
      {

        var identifierFut = fingerTable(i)._2 ? GetIdentifier
        Await.result(identifierFut,Duration.Inf)
        var node = identifierFut.value.get.get.asInstanceOf[Long]

        if(inInterval(node,identifier,id))
        {
          //println("in IF  -> "  + fingerTable(i)._2)
          sender ! fingerTable(i)._2
          //context.stop(self)
        }
      }
      //println("out of if -> " + sender)
      sender ! nodeRef
      //context.stop(self)
    }
  }
}




object  ClosestFingerPreceding
{
  trait Reqest
  case class Calculate(id: Long, fingerTable: List[(Long, ActorRef)], nodeRef: ActorRef) extends Reqest


  def props(keyspace: Int):Props = Props(new ClosestFingerPreceding(keyspace))

}