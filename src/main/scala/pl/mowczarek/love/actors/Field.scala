package pl.mowczarek.love.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.mowczarek.love.actors.CreatureActor.{Accost, Migrate}
import pl.mowczarek.love.actors.Field.{MigrateCreature, SpawnCreature}
import pl.mowczarek.love.actors.SystemMap.GetRandomField
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

/**
  * Created by neo on 22.03.17.
  */
class Field(x: Int, y: Int, gameMap: ActorRef) extends Actor with ActorLogging {

  private var creatures: Set[ActorRef] = Set()

  implicit val timeout: Timeout = 5 seconds

  override def preStart = {
    log.info(s"Field created: x: $x, y: $y")
  }

  override def receive: Receive = {
    case SpawnCreature =>
      creatures += sender
      log.info(s"Creature landed on field ($x, $y)")

    case command @ Accost(attributes) =>
      creatures.filter(_ != sender).foreach { creatureRef =>
        creatureRef forward command
      }

    case command @ Migrate =>
      (gameMap ? GetRandomField).mapTo[ActorRef].onComplete {
        case Failure(ex) => throw ex
        case Success(field: ActorRef) =>
          creatures -= sender
          log.info(s"Creature left field ($x, $y)")
          field ! MigrateCreature(sender)
      }

    case MigrateCreature(creatureActor) =>
      creatures += creatureActor
      log.info(s"Creature migrated onto field ($x, $y)")
  }
}

object Field {
  case object SpawnCreature
  case class MigrateCreature(creature: ActorRef)
  case class MoveCreature(dx: Int, dy: Int)

  def props(x: Int, y: Int, gameMap: ActorRef) = Props(new Field(x, y, gameMap))
}
