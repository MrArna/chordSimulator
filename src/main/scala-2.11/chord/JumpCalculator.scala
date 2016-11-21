package chord

import akka.actor.{Actor, ActorRef, Props}
import simulator.ClusterManager.NextNode
import chord.JumpCalculator.{Calculate, InitObserver, JoinObserver}
import chord.Node.JoinCompleted

/**
  * Created by Marco on 21/11/16.
  */
class JumpCalculator(numNodes: Int, numRequests: Int) extends Actor
{
  var totalHops: Double = 0
  var totalRequests: Double = numNodes * numRequests
  var receivedReq: Double = 0
  var masterNode:ActorRef = null
  var joinCount:Int = 0
  var currentNodes:List[Int]= List()
  var currentJoinedCount:Int = 0
  def receive = {

    case Calculate(hCount: Int) => {
      totalHops += hCount
      receivedReq += 1

      if (receivedReq == totalRequests) {

        var avgHops: Double = totalHops / totalRequests
        println("All Requests Completed...")
        println("Total Hops:" + totalHops)
        println()
        println("Average Number Hops: " + avgHops)

      }

    }

    case InitObserver(mNode:ActorRef, jCount:Int)=>{
      masterNode = mNode
      joinCount = jCount

    }

    case JoinObserver(cNodes:List[Int], currentJCount:Int)=>{
      currentNodes = cNodes
      currentJoinedCount +=1

      if(currentJoinedCount == joinCount ){
        sender ! JoinCompleted(currentNodes)

      }else{
        masterNode ! NextNode(currentJoinedCount)
      }
    }
  }
}


object JumpCalculator
{
  trait Request
  case class JoinObserver(cNodes:List[Int], currentJCount:Int) extends Request
  case class InitObserver(mNode:ActorRef, jCount:Int) extends Request
  case class Calculate(jumoCount: Int) extends Request

  trait Response


  def props(numNodes: Int, numRequests: Int):Props = Props(new JumpCalculator(numNodes, numRequests))
}
