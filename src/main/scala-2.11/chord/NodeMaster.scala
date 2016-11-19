package chord

import java.math.BigInteger

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import chord.Node._
import chord.algorithms.Join.JoinCompleted

import scala.util.Random



/**
  * Created by Marco on 16/11/16.
  */
class ClusterManager(keyspace: Int) extends Actor with ActorLogging {


  import ClusterManager._

  import scala.concurrent.duration._
  implicit val timeout = Timeout(60 seconds)

  //require(keyspace > 0, "keyspaceBits must be a positive Int value")

  private val idModulus = 1 << keyspace
  private var nodes: Map[Int, ActorRef] = Map.empty

  private var joinRequestQueue: Map[Int,ActorRef] = Map.empty


  private var keys: Set[Int] = Set.empty

  private def generateKey(): Int = {
    val keyString = "Marco" + "Arnaboldi" + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256)
    val md = java.security.MessageDigest.getInstance("SHA-1")
    var encodedString = md.digest(keyString.getBytes("UTF-8")).map("%02x".format(_)).mkString
    encodedString = new BigInteger(encodedString, 16).toString(2)
    var keyHash = Integer.parseInt(encodedString.substring(encodedString.length() - idModulus), 2)
    if (keys.contains(keyHash)) {
      keyHash = generateKey()
    }
    return keyHash
  }


  private def generateUniqueId(nodeIds: Set[Int]): Int = {
    val nodeIP = scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256)
    val md = java.security.MessageDigest.getInstance("SHA-1")
    var encodedString = md.digest(nodeIP.getBytes("UTF-8")).map("%02x".format(_)).mkString
    encodedString = new BigInteger(encodedString, 16).toString(2)

    var addressHash = Integer.parseInt(encodedString.substring(encodedString.length() - keyspace), 2)
    if (nodeIds.contains(addressHash)) {
      addressHash = generateUniqueId(nodeIds)
    }
    return addressHash
  }


  override def receive: Receive = {

    case CreateNodeRequest =>
    {
      val nodeId: Int = generateUniqueId(nodes.keySet | joinRequestQueue.keySet)
      val nodeActor = context.actorOf(Node.props(nodeId,keyspace,self))
      joinRequestQueue = joinRequestQueue + (nodeId -> nodeActor)

      if (joinRequestQueue.size == 1)
      {
        if (nodes.isEmpty)
        {
          joinRequestQueue(nodeId) ! LetJoin(nodeActor)
        }
        else
        {
          var existingNodeId = nodes.keySet.toVector(Random.nextInt(nodes.keySet.size))
          while(existingNodeId == nodeId)
          {
            existingNodeId = nodes.keySet.toVector(Random.nextInt(nodes.keySet.size))
          }
          joinRequestQueue(existingNodeId) ! LetJoin(nodeActor)
        }
      }
    }


    case InitCluster(numNodes) =>
    {
      for (i <- 0 until numNodes)
      {
        self ! CreateNodeRequest
      }
    }

    case TestMessage => nodes.head._2 ! InvokeFindSuccessor(4)

    case Dump => for ((id, ref) <- nodes) ref ! DumpState

    case JoinCompleted(nodeId) =>
    {
      val actorRef: ActorRef = joinRequestQueue.get(nodeId.toInt).get
      nodes = nodes + (nodeId.toInt -> actorRef)
      joinRequestQueue = joinRequestQueue - nodeId.toInt
      if(!joinRequestQueue.isEmpty)
      {
        val head = joinRequestQueue.head
        val nodeActor = head._2
        val existingNodeId = nodes.keySet.toVector(Random.nextInt(nodes.keySet.size))
        nodes(existingNodeId) ! LetJoin(nodeActor)
      }
    }
  }
}

object ClusterManager
{
  sealed trait Request
  case object CreateNodeRequest extends Request
  case class InitCluster(numNodes: Int) extends Request
  case object TestMessage extends Request
  case object Dump


  sealed trait Response
  case object InitCompleted extends Response


}
