package patterns.patterns.actors

import akka.actor.typed.ActorSystem

object Main extends App {
  val supervisor = ActorSystem(SupervisorActor(), "supervisor")
  supervisor ! SupervisorActor.Start
}
