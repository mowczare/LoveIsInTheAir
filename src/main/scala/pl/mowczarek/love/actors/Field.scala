package pl.mowczarek.love.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.mowczarek.love.actors.CreatureActor.Accost
import pl.mowczarek.love.actors.Field.SpawnCreature

/**
  * Created by neo on 22.03.17.
  */
class Field(x: Int, y: Int, gameMap: ActorRef) extends Actor with ActorLogging {

  private var creatures: Set[ActorRef] = Set()

  override def preStart = {
    log.info(s"Field created: x: $x, y: $y")
  }

  override def receive: Receive = {
    case SpawnCreature =>
      creatures += sender
      log.info(s"Creature landed on field ($x, $y)")

    case Accost(attributes) =>
      creatures.filter(_ != sender).foreach { creatureRef =>
        creatureRef ! Accost(attributes)
      }
  }
}

object Field {
  case object SpawnCreature
  case class MoveCreature(dx: Int, dy: Int)

  def props(x: Int, y: Int, gameMap: ActorRef) = Props(new Field(x, y, gameMap))
}
