package chord

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import chord.Node.DumpState
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import simulator.ClusterManager.NextNode

/**
  * Created by Marco on 21/11/16.
  */
class NodeTest
  extends TestKit(ActorSystem("NodeTest",ConfigFactory.parseString(TestKitUsageSpec.config)))
with DefaultTimeout
with ImplicitSender
with WordSpecLike
with Matchers
with BeforeAndAfterAll
{

  "A Node" should {
    "be initialized and provide methods to let other nodes join" in {
      import scala.concurrent.duration._
      within(20 seconds) {
        //2 nodes 2 and 3
        val nodes = Array.ofDim[ActorRef](8)
        val node1 = TestActorRef(Node.props(3))
        val node2 = TestActorRef(Node.props(3))
        val jumpCalc = TestActorRef(JumpCalculator.props(2, 20))
        jumpCalc ! JumpCalculator.InitObserver(self, 2)

        nodes(2) = node1
        nodes(3) = node2

        val fingerTable1 = new Array[String](3)
        fingerTable1(0) = "3,3"
        fingerTable1(1) = "4,2"
        fingerTable1(2) = "6,2"
        val fingerTable2 = new Array[String](3)
        fingerTable2(0) = "4,2"
        fingerTable2(1) = "5,2"
        fingerTable2(2) = "7,2"

        node1 ! Node.Initialize(2, 3, 3, fingerTable1, 3, List(2, 4, 5, 6, 7, 0, 1), jumpCalc)
        node2 ! Node.Initialize(3, 2, 2, fingerTable2, 3, List(3), jumpCalc)



        val node3 = TestActorRef(Node.props(3))
        nodes(5) = node3

        node3 ! Node.Join(5, node2, nodes, 10, jumpCalc)

        expectMsg(NextNode(1))

        node3 ! DumpState
        node2 ! DumpState
        node1 ! DumpState

      }
    }

  }

}
