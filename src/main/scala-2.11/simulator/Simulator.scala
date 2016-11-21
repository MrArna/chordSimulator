package simulator

import java.math.BigInteger

import akka.actor.{ActorRef, ActorSystem}
import simulator.ClusterManager.{DumpSystem, InitMaster}


object Simulator extends App {


  val usage = """
    Usage:    -in <initial-nodes>
              -jn <joining-nodes>
              -d <duration-in-seconds>
              -ts <time-stamp-in-seconds>
              -qa <nr-of-quering-actors>
              -qn <nr-of-queries-per-actor>
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
      case "-qa" :: value :: tail => nextOption(map ++ Map('queringActor -> value.toInt), tail)
      case "-qn" :: value :: tail => nextOption(map ++ Map('queriesNumber -> value.toInt), tail)
      case string :: opt2 :: tail if isSwitch(opt2) => nextOption(map ++ Map('infile -> string), list.tail)
      case string :: Nil =>  nextOption(map ++ Map('infile -> string), list.tail)
    }
  }

  if (args.length == 0 || args.length != 6*2) {
    println(usage)
    System.exit(0)
  }
  val arglist = args.toList

  val options = nextOption(Map(), arglist)

  val numNodes: Int = options.get('initNodes).get.asInstanceOf[Int]
  val numJoiningNodes = options.get('joinNodes).get.asInstanceOf[Int]
  val numRequests: Int = 10 //options.get('queriesNumber).get.asInstanceOf[Int]


  val system = ActorSystem("ChordSystem")

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


  println()
  println("Network Build Started...")
  println("Joining Nodes: " + joiningNode.toList)
  //val tempList: List[Int] = 6:: 5:: 15:: 12:: 2:: Nil
  //val tempJoinList: List[Int] = 1::3::11::8:: 10:: Nil
  val master: ActorRef = system.actorOf(ClusterManager.props(keyspace))
  master ! InitMaster(nodeIDList, numRequests, joiningNode)

  while(true)
    {
      Console.readLine()
      master ! DumpSystem
    }
}