package patterns.patterns.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

object ItemStateActor {
  import domain._

  trait Ack
  object Ack extends Ack

  sealed trait Command
  final case class Init(ackTo: ActorRef[Ack]) extends Command
  final case class ItemUpdate(ackTo: ActorRef[Ack], item: ItemUpdated) extends Command
  final case object Complete extends Command
  final case class Fail(ex: Throwable) extends Command
  final case class GetItems(items: List[Int], replayTo: ActorRef[Response]) extends Command

  sealed trait Response
  final case class ItemResponse(items: Map[Int, Double]) extends Response
  final case class ItemUpdated(itemId: Int, newPrice: Double)

  def apply(): Behavior[Command] =
    Behaviors.setup { _ =>
      processUpdate(Map.empty[Int, Item])
    }

  def processUpdate(state: Map[Int, Item]): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Init(ackTo) =>
          ctx.log.info("Stream initialized.")
          ackTo ! Ack
          Behaviors.same
        case ItemUpdate(ackTo, itemUpdated) =>
          ctx.log.info("Message received")
          state.get(itemUpdated.itemId) match {
            case Some(item) =>
              val newItem = item.copy(price = itemUpdated.newPrice)
              val newState = state + (itemUpdated.itemId -> newItem)
              ackTo ! Ack
              processUpdate(newState)
            case None =>
              val item = Item(itemUpdated.itemId, itemUpdated.newPrice)
              val newState = state + (itemUpdated.itemId -> item)
              ackTo ! Ack
              processUpdate(newState)
          }
        case Complete =>
          ctx.log.info("Stream completed")
          Behaviors.same
        case Fail(ex) =>
          ctx.log.error("Stream failed")
          Behaviors.same
        case GetItems(items, replayTo) =>
          val currentItems = items.map(id => state.get(id)).collect { case Some(i) => i }
          val response = currentItems.map(item => item.id -> item.price).toMap
          replayTo ! ItemResponse(response)
          Behaviors.same
      }
    }
}
