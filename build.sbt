name := "Receiver"

version := "1.2"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.akka" %% "akka-stream" % "2.4.2",
  "com.typesafe.akka" %% "akka-actor" % "2.4.2",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.2",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)


//enablePlugins(SbtNativePackager)

enablePlugins(JavaAppPackaging)

