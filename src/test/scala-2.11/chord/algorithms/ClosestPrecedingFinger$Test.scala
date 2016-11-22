package chord.algorithms

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import chord.Node.SetInstance
import chord.{JumpCalculator, Node, TestKitUsageSpec}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global



/**
  * Created by Marco on 22/11/16.
  */
class ClosestPrecedingFinger$Test extends TestKit(ActorSystem("CFPTest",ConfigFactory.parseString(TestKitUsageSpec.config)))
  with DefaultTimeout
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
{

  "The CFP algorithm" should {
    "shoudl return the closest finger preciding an id" in {
      import scala.concurrent.duration._
      within(20 seconds) {
        //2 nodes 2 and 3
        val nodes = Array.ofDim[ActorRef](8)
        val nodesObj = Array.ofDim[Node](8)
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

        val nodeObj1Fut = node1 ? Node.GetInstance
        nodeObj1Fut onSuccess
          {
            case result: SetInstance => {
              nodesObj(result.nodeId) = result.receivedObj
              val nodeObj2Fut = node2 ? Node.GetInstance

              nodeObj2Fut onSuccess
                {
                  case result2: SetInstance => {
                    nodesObj(result2.nodeId) = result2.receivedObj
                    assert(ClosestPrecedingFinger.run(2, 4, nodesObj) == 3)
                  }
                }
            }
          }
        Await.result(nodeObj1Fut,Duration.Inf)
      }
    }

  }

}