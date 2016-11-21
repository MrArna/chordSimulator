package chord

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import chord.ClusterManager._

import scala.concurrent.duration._


object Test extends App
{
  val system = ActorSystem("Test")
  implicit val timeout = Timeout(60 seconds)

  val cluster = system.actorOf(Props(new ClusterManager(3)))

  println("Init completed\n")
  cluster ! InitCluster(3)

  while (true)
    {
      scala.io.StdIn.readLine()
      cluster ! Dump
    }

  //cluster ! TestMessage
}