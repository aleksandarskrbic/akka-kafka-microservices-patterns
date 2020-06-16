package patterns.actors

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object Application extends App {

  val device: ActorSystem[DeviceActor.Command] = ActorSystem(DeviceActor(1L), "device-1")
  implicit val ec = device.executionContext
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler = device.scheduler

  device ! DeviceActor.UpdateAirQuality(10)
  device ! DeviceActor.UpdateAirQuality(20)
  device ! DeviceActor.UpdateTemperature(20)
  device ! DeviceActor.UpdateTemperature(10)

  val response: Future[DeviceActor.Response] = device.ask(ref => DeviceActor.StateRequest(ref))

  response.onComplete {
    case Success(DeviceActor.StateResponse(state)) =>
      println("Current state is: " + state.toString)
      device.terminate()
    case Failure(_) =>
      println("Unable to get current state.")
      device.terminate()
  }

}
