package patterns.patterns.rl

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout

import scala.concurrent.duration._

object BatchExample extends App {

  implicit val dbActor = ActorSystem(DatabaseActor(), "db-actor")
  implicit val ec = dbActor.executionContext
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler = dbActor.scheduler

  val records = (1 to 1000000).map(i => s"record-${i - 1}")

  def insertPipeline() =
    Source(records).map(DatabaseActor.Insert).map(dbActor ! _).runWith(Sink.ignore)

  def batchInsertPipeline() =
    Source(records)
      .groupedWithin(1000, 1.second)
      .map(batch => DatabaseActor.InsertBatch(batch.toList))
      .map(dbActor ! _)
      .runWith(Sink.ignore)
}
