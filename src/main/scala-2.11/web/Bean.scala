package web

import akka.actor.{ActorRef, ActorRefFactory, Props}
import spray.can.websocket
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.{BinaryFrame, TextFrame}
import spray.http.HttpRequest
import spray.routing.HttpServiceActor

/**
  * Created by Marco on 16/11/16.
  */
class Bean(val serverConnection: ActorRef, val governor: ActorRef)
  extends HttpServiceActor with websocket.WebSocketServerWorker with BeanBehaviour {

  private def routesWithEventStream = routes(governor) ~ pathPrefix("eventstream") {
    getFromResourceDirectory("webapp")
  }

  private def businessLogicNoUpgrade: Receive = {
    implicit val refFactory: ActorRefFactory = context
    runRoute(routesWithEventStream)
  }

  override def businessLogic: Receive = {
    case x@(_: BinaryFrame | _: TextFrame) => // Bounce back
      sender() ! x

    /*case e: Event =>
      e match {
        case FingerReset(nodeId: Long, index: Int) =>
          send(TextFrame(s"""{ "type": "FingerReset", "nodeId": $nodeId, "index": $index }"""))
        case FingerUpdated(nodeId: Long, index: Int, fingerId: Long) =>
          send(TextFrame(s"""{ "type": "FingerUpdated", "nodeId": $nodeId, "index": $index, "fingerId": $fingerId }"""))
        case NodeCreated(nodeId, successorId) =>
          send(TextFrame(s"""{ "type": "NodeCreated", "nodeId": $nodeId, "successorId": $successorId }"""))
        case NodeShuttingDown(nodeId) =>
          send(TextFrame(s"""{ "type": "NodeDeleted", "nodeId": $nodeId }"""))
        case PredecessorReset(nodeId) =>
          send(TextFrame(s"""{ "type": "PredecessorReset", "nodeId": $nodeId }"""))
        case PredecessorUpdated(nodeId, predecessorId) =>
          send(TextFrame(s"""{ "type": "PredecessorUpdated", "nodeId": $nodeId, "predecessorId": $predecessorId }"""))
        case SuccessorListUpdated(nodeId, primarySuccessorId, _) =>
          send(TextFrame(s"""{ "type": "SuccessorUpdated", "nodeId": $nodeId, "successorId": $primarySuccessorId }"""))
      }*/

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case x: HttpRequest => // do something
  }

  override def receive: Receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic
}

object Bean {
  def props(serverConnection: ActorRef, nodeRef: ActorRef): Props =
    Props(new Bean(serverConnection, nodeRef))
}