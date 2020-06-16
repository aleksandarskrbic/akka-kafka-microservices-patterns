name := "akka-kafka-microservices-patterns"
version := "0.1"
scalaVersion := "2.13.2"

lazy val global = project.in(file(".")).aggregate(examples)

lazy val examples = (project in file("examples")).settings(
  name := "examples",
  libraryDependencies ++= Seq(
      dependencies.akkaActors,
      dependencies.akkaStreams,
      dependencies.akkaHttp,
      dependencies.akkaSprayJson,
      dependencies.alpakkaKafka,
      dependencies.logback))

lazy val dependencies = new {
  val akkaVersion = "2.6.6"
  val akkaHttpVersion = "10.1.12"
  val alpakkaVersion = "2.0.3"
  val logbackVersion = "1.2.2"
  val scalaLoggingVersion = "3.9.2"

  val akkaActors = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaStreams = "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
  val alpakkaKafka = "com.typesafe.akka" %% "akka-stream-kafka" % alpakkaVersion
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
}

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
