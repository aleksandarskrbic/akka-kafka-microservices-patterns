package patterns.patterns.rl

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object DatabaseActor {

  sealed trait Command
  final case class Insert(value: String) extends Command
  final case class InsertBatch(values: List[String]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { _ =>
      process(0, Map.empty[Int, String])
    }

  def process(nextId: Int, state: Map[Int, String]): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Insert(value) =>
          ctx.log.info(s"Received $value")
          val newState = state + (nextId -> value)
          process(nextId + 1, newState)
        case InsertBatch(values) =>
          ctx.log.info(s"Received batch: $values")
          val res = values.zipWithIndex.map { case (value, idx) => (idx + nextId) -> value }.toMap
          process(nextId + values.length, state ++ res)
      }
    }
}
