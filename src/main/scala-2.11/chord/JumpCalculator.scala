package chord

import akka.actor.{Actor, ActorRef, Props}
import simulator.ClusterManager.NextNode
import chord.JumpCalculator._
import chord.Node.JoinCompleted

/**
  * Created by Marco on 21/11/16.
  */
class JumpCalculator(numNodes: Int, numRequests: Int) extends Actor
{
  var totalJumps: Double = 0
  var receivedReq: Double = 0
  var clusterManager:ActorRef = null
  var joinCount:Int = 0
  var currentNodes:List[Int]= List()
  var currentJoinedCount:Int = 0
  var requestCompleted = 0

  def receive = {

    case Calculate => {
      var avgJumos: Double = 0
      if(requestCompleted != 0) avgJumos = totalJumps / requestCompleted
      println()
      println("Total Jumps:" + totalJumps + " Total Requests Completed: " + requestCompleted)
      println()
      println("Average Number Jumps: " + avgJumos)
      println()
      sender ! "ok"

    }

    case JumpDone =>
      {
        totalJumps += 1
      }

    case RequestCompleted => requestCompleted += 1


    case InitObserver(mNode:ActorRef, jCount:Int)=>{
      clusterManager = mNode
      joinCount = jCount

    }

    case JoinObserver(cNodes:List[Int], currentJCount:Int)=>{
      currentNodes = cNodes
      currentJoinedCount +=1

      if(currentJoinedCount == joinCount ){
        sender ! JoinCompleted(currentNodes)

      }else{
        clusterManager ! NextNode(currentJoinedCount)
      }
    }
  }
}


object JumpCalculator
{
  trait Request
  case class JoinObserver(cNodes:List[Int], currentJCount:Int) extends Request
  case class InitObserver(mNode:ActorRef, jCount:Int) extends Request
  case object Calculate extends Request
  case object RequestCompleted extends Request
  case object JumpDone extends Request

  trait Response


  def props(numNodes: Int, numRequests: Int):Props = Props(new JumpCalculator(numNodes, numRequests))
}