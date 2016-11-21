package chord.algorithms

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import chord.Node.{GetFingerTable, GetIdentifier}
import chord.algorithms.ClosestFingerPreceding.Calculate

import scala.concurrent.Await

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
    case Calculate(id,nodeRef) =>
    {
      println("-> CFP invoked")

      //return node
      var result:ActorRef = nodeRef

      // n
      var identifierFut = nodeRef  ? GetIdentifier
      Await.result(identifierFut,Duration.Inf)
      val identifier = identifierFut.value.get.get.asInstanceOf[Long]


      //n.finger_table
      val fingerTableFut = nodeRef ? GetFingerTable
      Await.result(fingerTableFut,Duration.Inf)
      val fingerTable = fingerTableFut.value.get.get.asInstanceOf[List[(Long,ActorRef)]]



      // for i = m downto 1
      for(i <- (keyspace-1) to 0 by -1)
      {
        //n.finger_table[i].node
        identifierFut = fingerTable(i)._2 ? GetIdentifier
        Await.result(identifierFut,Duration.Inf)
        var node = identifierFut.value.get.get.asInstanceOf[Long]


        //if n.finger_table[i].node belongs to [n, id]
        if(inInterval(node,identifier,id))
        {
          //return finger_Table[i].node
          result = fingerTable(i)._2
        }
      }

      sender ! result
    }
  }
}




object  ClosestFingerPreceding
{
  trait Reqest
  case class Calculate(id: Long, nodeRef: ActorRef) extends Reqest


  def props(keyspace: Int):Props = Props(new ClosestFingerPreceding(keyspace))

}