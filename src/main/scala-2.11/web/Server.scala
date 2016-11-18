package web

import akka.actor.{Actor, ActorRef, Props}
import spray.can.Http

/**
  * Created by Marco on 16/11/16.
  */

class Server (val governor: ActorRef) extends Actor {

  override def receive: Receive = {
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(Bean.props(serverConnection, governor))
      serverConnection ! Http.Register(conn)

    //case e: Event =>
      //context.children.foreach { _ ! e }
  }
}

object Server {
  def props(governor: ActorRef): Props = Props(new Server(governor))
}