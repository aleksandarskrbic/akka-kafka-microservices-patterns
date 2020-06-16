package patterns.patterns.actors

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSink

object SupervisorActor {

  sealed trait Command
  final object Start extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      implicit val system = ctx.system
      val itemStateActor = ctx.spawn(ItemStateActor(), "item-state-actor")
      val orderActor = ctx.spawn(OrderActor(itemStateActor), "order-actor")
      val itemSink = createItemSink(itemStateActor)

      import queue._
      val itemUpdateTopic = Source(itemsUpdate).map(item => ItemStateActor.ItemUpdated(item.id, item.price))
      val orderTopic = Source(placedOrders).map(order => OrderActor.OrderEvent(order))

      def process(): Behavior[Command] =
        Behaviors.receiveMessage {
          case Start =>
            itemUpdateTopic.runWith(itemSink)
            orderTopic.runForeach(orderActor ! _)
            Behaviors.same
        }

      process()
    }

  private def createItemSink(actor: ActorRef[ItemStateActor.Command]) =
    ActorSink.actorRefWithBackpressure(
      ref = actor,
      onCompleteMessage = ItemStateActor.Complete,
      onFailureMessage = ItemStateActor.Fail.apply,
      messageAdapter = ItemStateActor.ItemUpdate.apply,
      onInitMessage = ItemStateActor.Init.apply,
      ackMessage = ItemStateActor.Ack)

}
