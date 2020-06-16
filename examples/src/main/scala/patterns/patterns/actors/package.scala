package patterns.patterns

package object actors {

  object domain {
    final case class Item(id: Int, price: Double)

    sealed class Order
    final case class OrderDetails(id: Int, userId: Int, items: List[Item])
    final case class PlacedOrder(details: OrderDetails) extends Order
    final case class OrderApproved(details: OrderDetails) extends Order
    final case class OrderRejected(details: OrderDetails) extends Order
  }

  object generator {
    import domain._

    val items = (1 to 10).map(i => Item(i, i * 10))
  }

  object queue {
    import domain._
    import generator._
    import scala.util.Random

    val itemsUpdate = items ++ items.filter(_.id > 5).map(item => item.copy(price = item.price * 2))
    val placedOrders = (1 to 10).map(i => PlacedOrder(OrderDetails(i, i, items.take(Random.nextInt(2) + 1).toList)))
  }
}
