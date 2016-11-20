package chord.algorithms

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import akka.pattern.ask
import chord.Node
import chord.Node.SetFingertable
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by Marco on 20/11/16.
  */
class FindPredecessorTest extends TestKit(ActorSystem("FindPredecessorTest",ConfigFactory.parseString(TestKitUsageSpec.config)))
  with DefaultTimeout
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
{

  "A FindPredecessor actor" should {
    "find the predecessor node to a given id" in {

      val node1 = TestActorRef(Node.props(6,3,null),name = "nodeSix")
      val node2 = TestActorRef(Node.props(7,3,null),name = "nodeSeven")

      val alg = TestActorRef(FindPredecessor.props(3))

      val fingerTable1 = List((7.toLong,node2),(0.toLong,node1),(2.toLong,node1))
      val fingerTable2 = List((0.toLong,node1),(1.toLong,node1),(3.toLong,node1))

      Await.result(node1 ? SetFingertable(fingerTable1),Duration.Inf)
      Await.result(node2 ? SetFingertable(fingerTable2),Duration.Inf)


      alg ! FindPredecessor.Calculate(3,node1)

      expectMsg(node2)


    }

  }

}
