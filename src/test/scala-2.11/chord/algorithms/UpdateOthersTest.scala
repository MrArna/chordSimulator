package chord.algorithms

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import akka.pattern.ask
import chord.Node
import chord.Node.{GetFingerTable, SetFingertable}
import chord.algorithms.InitFingerTable.InitFingerTableCompleted
import chord.algorithms.UpdateOthers.UpdateOthersCompleted
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by Marco on 20/11/16.
  */
class UpdateOthersTest
  extends TestKit(ActorSystem("UpdateOthers",ConfigFactory.parseString(TestKitUsageSpec.config)))
    with DefaultTimeout
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  "A UpdateOthers actor" should {
    "update the finger tables of other nodes" in {

      val node1 = TestActorRef(Node.props(6, 3, null), name = "nodeSix")
      val node2 = TestActorRef(Node.props(7, 3, null), name = "nodeSeven")

      val alg = TestActorRef(InitFingerTable.props(3))
      val alg2 = TestActorRef(UpdateOthers.props(3))
      //val fingerTable1 = List((7.toLong,node1),(0.toLong,node1),(2.toLong,node1))
      val fingerTable2 = List((0.toLong, node2), (1.toLong, node2), (3.toLong, node2))

      //Await.result(node1 ? SetFingertable(fingerTable1),Duration.Inf)
      Await.result(node2 ? SetFingertable(fingerTable2), Duration.Inf)
      Await.result(alg ? InitFingerTable.Calculate(node2, node1), Duration.Inf)
      Await.result(alg2 ? UpdateOthers.Calculate(node1), Duration.Inf)

      node2 ? GetFingerTable onComplete
        {
          result2 => println(result2)
        }
    }
  }
}
