package chord

import java.math.BigInteger

import akka.actor.{Actor, ActorLogging, ActorRef}
import chord.Node.{LetJoin, TestFindSuccessor}

import scala.util.Random



/**
  * Created by Marco on 16/11/16.
  */
class ClusterManager(keyspace: Int) extends Actor with ActorLogging {


  import ClusterManager._

  //require(keyspace > 0, "keyspaceBits must be a positive Int value")

  private val idModulus = 1 << keyspace
  private var nodes: Map[Int, ActorRef] = Map.empty
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
      val nodeId: Int = generateUniqueId(nodes.keySet)
      val nodeActor = context.actorOf(Node.props(nodeId,keyspace))
      nodes = nodes + (nodeId -> nodeActor)
      var existingNodeId = nodes.keySet.toVector(Random.nextInt(nodes.keySet.size))
      while(existingNodeId == nodeId)
      {
        existingNodeId = nodes.keySet.toVector(Random.nextInt(nodes.keySet.size))
      }
      nodes(existingNodeId) ! LetJoin(nodeActor)
    }


    case InitCluster(numNodes) =>
    {
      val firstNodeId = generateUniqueId(nodes.keySet)
      val nodeActor = context.actorOf(Node.props(firstNodeId,keyspace))
      nodes = nodes + (firstNodeId -> nodeActor)
      sender ! InitCompleted

    }

    case TestMessage => nodes.head._2 ! TestFindSuccessor(4)


  }
}

object ClusterManager
{
  sealed trait Request
  case object CreateNodeRequest extends Request
  case class InitCluster(numNodes: Int) extends Request
  case object TestMessage extends Request



  sealed trait Response
  case object InitCompleted extends Response


}
