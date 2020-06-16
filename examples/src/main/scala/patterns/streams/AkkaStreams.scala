package patterns.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source

object AkkaStreams extends App {

  implicit val system = ActorSystem("AkkaStreams")

  val tweets = List("#Scala", "#akka", "#JVM", "#Kafka")

  val control = Source(tweets).map(_.substring(1)).map(_.toLowerCase).filter(_.length > 3).runForeach(println)

}
