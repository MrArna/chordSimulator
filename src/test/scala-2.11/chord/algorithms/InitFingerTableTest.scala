package chord.algorithms

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import akka.pattern.ask
import chord.Node
import chord.Node.SetFingertable
import chord.algorithms.InitFingerTable.InitFingerTableCompleted
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by Marco on 20/11/16.
  */
class InitFingerTableTest
  extends TestKit(ActorSystem("InitFingerTableTest",ConfigFactory.parseString(TestKitUsageSpec.config)))
  with DefaultTimeout
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
{

  "A InitFingerTable actor" should {
    "initialize the finger table of a given node" in {

      val node1 = TestActorRef(Node.props(6,3,null),name = "nodeSix")
      val node2 = TestActorRef(Node.props(7,3,null),name = "nodeSeven")

      val alg = TestActorRef(InitFingerTable.props(3))

      //val fingerTable1 = List((7.toLong,node1),(0.toLong,node1),(2.toLong,node1))
      val fingerTable2 = List((0.toLong,node2),(1.toLong,node2),(3.toLong,node2))

      //Await.result(node1 ? SetFingertable(fingerTable1),Duration.Inf)
      Await.result(node2 ? SetFingertable(fingerTable2),Duration.Inf)


      alg ! InitFingerTable.Calculate(node2,node1)

      expectMsg(InitFingerTableCompleted)


    }

  }

}