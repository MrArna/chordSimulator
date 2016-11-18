package web

import akka.actor.ActorRef
import spray.http.{MediaTypes, StatusCodes}
import spray.routing.HttpService

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by Marco on 16/11/16.
  */

trait BeanBehaviour extends HttpService {


  implicit def ec: ExecutionContextExecutor = actorRefFactory.dispatcher

  private def messageForInternalServerError =
    "The request failed due to an internal server error. Details will be available in the server logs."

  protected def routes(governor: ActorRef) = pathPrefix("start") {
    pathEndOrSingleSlash {
      get {
        respondWithMediaType(MediaTypes.`application/json`) {
            complete("Succesfully started")
          }
        }
      }
    }
}
