CS441 @ UIC: HOMEWORK4
======================
Developed by Marco Arnaboldi (marnab2@uic.edu)

#Description
--------------------
In your fourth and the final homework assignment you will solidify the knowledge of resilient overlay networks by designing and implementing a simulation of a cloud computing facility, specifically a reliable overlay network using the Chord algorithm for distribution of work in a cloud datacenter. Your goal is to gain experience with the fundamentals of distributed hash tables (DHTs) and reallocation of resources in the cloud environment. You will implement a cloud simulator in Scala using Akka actors and you will build and run your project using the SBT with the runMain command from the command line. In your cloud simulator, you will create the following entities and define interactions among them: actors that simulate users who enter and retrieve data from the cloud, actors who represent computers (i.e., nodes) in the cloud that store the data, and case classes that represent data that are sent to and retrieved from the cloud. The entry point to your simulated cloud will be defined with a RESTful service using Akka/HTTP. You will use the latest community version of IntelliJ IDE for this assignment, as usual.

#Development & Design choices
-----------------

######Application
The behaviour wat thought as follow: a initial network i build offline. Then a number of nodes join to the network and then the request phase starts.

The application was developed with with IntelliJIDEA IDE. SBT was also exploited in order to manage the libraries. In particular it was developed using the following environment: OS X 10 native.
The application was written in Scala, adopting the Akka framework.

It has been designed in order to be as extendable as possible. In detail, it's composed by 2 modules composed by submodules and/or classes:

+ **Simulator**: contains the object in charge to launch the simulation and a class in order to manage the entire cluster
    + *Simulator*: this object parses the args passed from the user and then instantiate the actor system with that configuration
    + *ClusterManager*: this class represents the underlying simulated network infrastructure
    
+ **Chord**: contains the node definition and the algorithms which characterize Chord
    + *Node*: this actor represents a node on the network. It has different structures in order to maintain the Chord ring. In particular each node has: an ID, a PRED and SUCC, KEYS and a FINGER_TABLE. In order to develop the system in an easier way also other redundant structures were added. E.g. each node is aware of all the other nodes thanks to a node list containing the ref to all the others node.
    + *JumpCalculatore*: this actor has a double behaviour. In first place is in charge to tell to the ClusterManager when the Join phase is completed and hence starting the querying phase. In second place is in order to retrieve metrics about the number of queries and average jump number.
    + *algorithms*: this package contains the implementation of the Chord algorithms. Further information can be found in the code.
  
Further information about classes can be found as comment into the code.

#Functionalities and limitations
----------------

#####Functionalities

The application creates a Chord based network, join to it nodes and send requests to them. Logging periodically information on the network

#####Limitation
The major limitations are: all the keys are assigned to the respective nodes, the application is launched by command line.


#Usage
----------------

#####Application

`sbt run '-in <initial-nodes> -jn <joining-nodes> -d <duration-in-seconds> -ts <time-stamp-in-seconds> -qn <nr-of-queries-per-node>'`
                        

#Test
----------------
##### TestKit
Automated tests with TestKit were made for node actor in order to prove the correct behaviour of the algorithms, since they are at the base of the Chord protocol. 



#Acknowledgments
---------------
Inspiration was taken by the Akka documentation and [tutorials provided online](https://www.youtube.com/watch?v=imNYRPO74R8) and other projects like [Open Chord](https://github.com/allenfromu/Open-Chord-Scala) or [Chord](https://github.com/nikhiltiware/Chord-Network-Protocol-Simulation). Code was rewritten and adapted in order to meet the specification and the design choices.