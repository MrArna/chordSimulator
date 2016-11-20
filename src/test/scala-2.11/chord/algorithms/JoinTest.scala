package chord.algorithms

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import akka.pattern.ask
import chord.Node
import chord.Node.GetFingerTable
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by Marco on 20/11/16.
  */
class JoinTest
  extends TestKit(ActorSystem("JoinTest",ConfigFactory.parseString(TestKitUsageSpec.config)))
    with DefaultTimeout
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  "A UpdateOthers actor" should {
    "update the finger tables of other nodes" in {

      val node1 = TestActorRef(Node.props(6, 3, null), name = "nodeSix")
      val node2 = TestActorRef(Node.props(7, 3, null), name = "nodeSeven")

      val alg = TestActorRef(Join.props(3,null))

      alg ! Join.Calculate(null,node2)
      //expectMsg(JoinCompleted(7))
      alg ! Join.Calculate(node2,node1)
      //expectMsg(JoinCompleted(6))

      node2 ? GetFingerTable onComplete
      {
        result2 => println(result2)
      }

      node1 ? GetFingerTable onComplete
        {
          result2 => println(result2)
        }

    }
  }
}