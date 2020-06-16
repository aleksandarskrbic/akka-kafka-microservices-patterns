package patterns.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

object DeviceActor {

  sealed trait Command
  final case class UpdateAirQuality(airQuality: Double) extends Command
  final case class UpdateTemperature(temperature: Double) extends Command
  final case class StateRequest(replyTo: ActorRef[Response]) extends Command

  sealed trait Response
  final case class StateResponse(state: State) extends Response

  final case class State(id: Long, airQuality: Double, temperature: Double)

  def apply(id: Long, airQuality: Double = 0.0, temperature: Double = 0.0): Behavior[Command] =
    Behaviors.setup { _ =>
      processMessage(State(id, airQuality, temperature))
    }

  def processMessage(state: State): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case UpdateAirQuality(airQuality) =>
          ctx.log.info(s"Updating air quality with value $airQuality")
          val newState = state.copy(airQuality = airQuality)
          processMessage(newState)
        case UpdateTemperature(temperature) =>
          ctx.log.info(s"Updating temperature with value $temperature")
          val newState = state.copy(temperature = temperature)
          processMessage(newState)
        case StateRequest(replyTo) =>
          ctx.log.info("Getting current state...")
          replyTo ! StateResponse(state)
          Behaviors.same
      }
    }
}
