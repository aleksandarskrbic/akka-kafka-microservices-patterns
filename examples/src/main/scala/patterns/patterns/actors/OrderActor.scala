package patterns.patterns.actors

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.Success

object OrderActor {
  import domain._

  sealed trait Command
  final case class OrderEvent(order: PlacedOrder) extends Command
  final case class AdaptedResponse(order: PlacedOrder, items: Map[Int, Double]) extends Command

  def apply(itemStateActor: ActorRef[ItemStateActor.Command]): Behavior[Command] =
    Behaviors.setup { ctx =>
      implicit val timeout: Timeout = 3.seconds
      implicit val system = ctx.system

      val approvedSink = Sink.foreach[OrderDetails](order => println(s"Order approved => $order"))
      val rejectedSink = Sink.foreach[OrderDetails](order => println(s"Order rejected => $order"))

      val processedOrdersQueue = Source
        .queue[Order](1000, OverflowStrategy.backpressure)
        .map {
          case OrderApproved(orderDetails) => Right(orderDetails)
          case OrderRejected(orderDetails) => Left(orderDetails)
        }
        .divertTo(approvedSink.contramap(_.right.get), _.isRight)
        .divertTo(rejectedSink.contramap(_.left.get), _.isLeft)
        .toMat(Sink.ignore)(Keep.left)
        .run()

      def process(): Behavior[Command] =
        Behaviors.receiveMessage {
          case OrderEvent(order) =>
            val items = order.details.items.map(_.id)
            ctx.ask(itemStateActor, ref => ItemStateActor.GetItems(items, ref)) {
              case Success(ItemStateActor.ItemResponse(updatedItems)) => AdaptedResponse(order, updatedItems)
            }
            Behaviors.same
          case AdaptedResponse(order, updatedItems) =>
            val currentItems = order.details.items
            val validatedItems = currentItems.filter(item => {
              updatedItems.get(item.id) match {
                case Some(price) if price == item.price => true
                case _                                  => false
              }
            })
            if (validatedItems.length == currentItems.length) {
              val approvedOrder = OrderApproved(order.details)
              processedOrdersQueue.offer(approvedOrder)
            } else {
              val rejectedOrder = OrderRejected(order.details)
              processedOrdersQueue.offer(rejectedOrder)
            }
            Behaviors.same
        }

      process()
    }
}
