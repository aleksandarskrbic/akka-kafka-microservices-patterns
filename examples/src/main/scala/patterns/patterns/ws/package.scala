package patterns.patterns

package object ws {
  import akka.actor.ActorSystem

  object KafkaOps {
    import akka.kafka.ProducerSettings
    import akka.kafka.scaladsl.Producer
    import org.apache.kafka.common.serialization.StringSerializer

    lazy val adTopic = "ad"

    def sink(implicit system: ActorSystem) = Producer.plainSink(jsonProducerSettings)

    private def jsonProducerSettings(implicit system: ActorSystem): ProducerSettings[String, String] = {
      ProducerSettings(system, new StringSerializer, new StringSerializer).withBootstrapServers("localhost:9092")
    }
  }

  object WebSocketOps {
    def webSocketUrl(implicit system: ActorSystem) = system.settings.config.getString("websocket.url")
  }

  object model {
    final case class WebSocketMessage(messageType: String, service: String, payload: List[Ad])
    final case class Ad(
        id: Long,
        url: String,
        name: String,
        thumbnail: String,
        price: String,
        posted: String,
        currency: String,
        lat: String,
        lon: String,
        location_id: Long,
        html: String)
  }

  object encoding {
    import model._
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
    import spray.json.DefaultJsonProtocol

    trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
      implicit val AdFormat = jsonFormat(
        Ad,
        "ad_id",
        "ad_url",
        "name",
        "thumbnail",
        "price",
        "posted",
        "currency",
        "lat",
        "lon",
        "location_id",
        "html")
      implicit val WebSocketMessageFormat = jsonFormat(WebSocketMessage.apply, "type", "service", "payload")
    }
  }
}
