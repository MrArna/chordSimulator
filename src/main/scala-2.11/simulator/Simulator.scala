package simulator

import java.math.BigInteger
import java.util.concurrent.TimeUnit

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import simulator.ClusterManager.{DumpSystem, InitMaster}
import spray.can.Http
import spray.can.server.UHttp
import spray.http.HttpRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.StdIn
import akka.pattern.ask
import akka.util.Timeout
import akka.actor._
import spray.can.Http
import spray.can.server.Stats
import spray.util._
import spray.http._
import HttpMethods._
import MediaTypes._
import spray.can.Http.RegisterChunkHandler




object Simulator extends App {

  implicit val timeout: Timeout = 3.seconds


  val usage = """
    Usage:    -in <initial-nodes>
              -jn <joining-nodes>
              -d <duration-in-seconds>
              -ts <time-stamp-in-seconds>
              -qn <nr-of-queries-per-actor-per-minute>
              """

  type OptionMap = Map[Symbol, Any]


  //read args and map values
  def nextOption(map : OptionMap, list: List[String]) : OptionMap = {
    def isSwitch(s : String) = (s(0) == '-')
    list match {
      case Nil => map
      case "-in" :: value :: tail => nextOption(map ++ Map('initNodes -> value.toInt), tail)
      case "-jn" :: value :: tail => nextOption(map ++ Map('joinNodes -> value.toInt), tail)
      case "-d" :: value :: tail => nextOption(map ++ Map('duration -> value.toInt), tail)
      case "-ts" :: value :: tail => nextOption(map ++ Map('timeStamp -> value.toInt), tail)
      case "-qn" :: value :: tail => nextOption(map ++ Map('queriesNumber -> value.toInt), tail)
      case string :: opt2 :: tail if isSwitch(opt2) => nextOption(map ++ Map('infile -> string), list.tail)
      case string :: Nil =>  nextOption(map ++ Map('infile -> string), list.tail)
    }
  }

  if (args.length == 0 || args.length != 5*2) {
    println(usage)
    System.exit(0)
  }
  val arglist = args.toList

  val options = nextOption(Map(), arglist)

  val numNodes: Int = options.get('initNodes).get.asInstanceOf[Int]
  val numJoiningNodes = options.get('joinNodes).get.asInstanceOf[Int]
  val numRequests: Int = options.get('queriesNumber).get.asInstanceOf[Int]

  println("*** PARAMS ***")
  println("Initial nodes: " + numNodes)
  println("Joining nodes: " + numJoiningNodes)
  println("Duration [SEC]: " + options.get('duration).get.asInstanceOf[Int])
  println("Logging frequency [SEC]: " + options.get('initNodes).get.asInstanceOf[Int])


  implicit val system = ActorSystem("ChordSystem")

  var nodeIDList: List[Int] = List()

  val keyspace: Int = Math.ceil(Math.log(numNodes+numJoiningNodes) / Math.log(2.0)).toInt

  var nodeSpace: Int = math.pow(2, keyspace).toInt

  var joiningNode: List[Int] = List()

  def getNodeID(): Int = {
    //generates a random IP
    val nodeIP = scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256) + "." + scala.util.Random.nextInt(256)
    val md = java.security.MessageDigest.getInstance("SHA-1")
    var encodedString = md.digest(nodeIP.getBytes("UTF-8")).map("%02x".format(_)).mkString
    encodedString = new BigInteger(encodedString, 16).toString(2)

    var addressHash = Integer.parseInt(encodedString.substring(encodedString.length() - keyspace), 2)
    //redo if the id already exists
    if (nodeIDList.contains(addressHash) || joiningNode.contains(addressHash)) {
      addressHash = getNodeID()
    }
    return addressHash
  }

  for(i <- 0 until numNodes)
  {
    nodeIDList ::= getNodeID()
  }

  for(i <- 0 until numJoiningNodes)
  {
    joiningNode ::= getNodeID()
  }

  class Server(master: ActorRef) extends Actor
  {
    override def receive: Receive =
    {
      case _: Http.Connected => sender ! Http.Register(self)

      case HttpRequest(GET, Uri.Path("/start"), _, _, _) =>
      {
        master ! InitMaster(nodeIDList, numRequests, joiningNode)
        println()
        println("-> Network Build Started")
        println("-> Joining Nodes: " + joiningNode.toList)
        system.scheduler.schedule(FiniteDuration(20, TimeUnit.SECONDS), FiniteDuration(options.get('initNodes).get.asInstanceOf[Int], TimeUnit.SECONDS), master, DumpSystem)
        system.scheduler.scheduleOnce(
          FiniteDuration(options.get('duration).get.asInstanceOf[Int],TimeUnit.SECONDS),
          new java.util.TimerTask {
            def run() {
              System.exit(0)}
          }
        )
        sender ! HttpResponse(entity = "Simulation started")
      }


    }
  }

  val master: ActorRef = system.actorOf(ClusterManager.props(keyspace))
  val server = system.actorOf(Props(new Server(master)))

  private val httpPortNumber = 4567
  IO(UHttp) ? Http.Bind(server, "localhost", httpPortNumber)
}


