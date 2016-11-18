name := "cs441-HW4"

version := "1.0"

scalaVersion := "2.11.7"

lazy val akkaVersion = "2.3.14"
lazy val scalatestVersion = "2.2.6"
lazy val sprayVersion = "1.3.3"
lazy val sprayJsonVersion = "1.3.2"
lazy val sprayWebsocketVersion = "0.1.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.wandoulabs.akka" %% "spray-websocket" % sprayWebsocketVersion,
  "io.spray" %% "spray-can" % sprayVersion,
  "io.spray" %% "spray-json" % sprayJsonVersion,
  "io.spray" %% "spray-routing-shapeless2" % sprayVersion,
  "io.spray" %% "spray-testkit" % sprayVersion % "test",
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)