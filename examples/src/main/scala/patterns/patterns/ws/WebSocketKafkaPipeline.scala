package patterns.patterns.ws

import spray.json._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{ Message, TextMessage, WebSocketRequest }
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Keep, Source }
import org.apache.kafka.clients.producer.ProducerRecord
import patterns.patterns.ws.encoding.JsonSupport
import patterns.patterns.ws.model.{ Ad, WebSocketMessage }

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

object WebSocketKafkaPipeline extends App with JsonSupport {

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher

  val queue = Source
    .queue[Message](1000, OverflowStrategy.backpressure)
    .mapAsyncUnordered(4)(extractAds)
    .map(_.map(toProducerRecord))
    .mapConcat(identity)
    .toMat(KafkaOps.sink)(Keep.left)
    .run()

  def extractAds(message: Message): Future[List[Ad]] =
    message match {
      case textMessage: TextMessage.Strict =>
        val text = textMessage.text
        val json = Try(text.parseJson.convertTo[WebSocketMessage]).toEither
        json.fold(_ => Future.successful(List.empty), m => Future.successful(m.payload))
      case _ => Future.successful(List.empty)
    }

  def toProducerRecord(ad: Ad): ProducerRecord[String, String] =
    new ProducerRecord(KafkaOps.adTopic, ad.id.toString, ad.toJson.prettyPrint)

  val webSocketFlow: Flow[Message, Message, NotUsed] =
    Flow[Message].mapAsync(1) {
      case message: TextMessage.Strict =>
        queue.offer(message).map(_ => message)
      case message => Future.successful(message)
    }

  Http().singleWebSocketRequest(WebSocketRequest(WebSocketOps.webSocketUrl), webSocketFlow)

  val control = queue.watchCompletion()
  control.onComplete {
    case Success(_) =>
      println("Stream completed successfully")
      system.terminate()
    case Failure(_) =>
      println("Stream failed")
      system.terminate()
  }
}
