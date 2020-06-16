package patterns.patterns

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }

import scala.collection.immutable.Seq
import scala.concurrent.Future

object ParallelProcessing extends App {
  implicit val system = ActorSystem("ParallelProcessing")
  implicit val ec = system.dispatcher

  final case class Item(id: Int, name: String)
  val items = (1 to 1000).map(i => Item(i, s"name-$i"))

  def process(item: Item): Future[Item] = {
    val newItem = item.copy(name = "changed - " + item.name)
    Future.successful(newItem)
  }

  val result: Future[Seq[Item]] =
    Source(items).mapAsync(4)(process).runWith(Sink.seq)

  val resultUnorderd: Future[Seq[Item]] =
    Source(items).mapAsyncUnordered(4)(process).runWith(Sink.seq)

  result.foreach(items => items.filter(_.id < 10).foreach(println))
}
