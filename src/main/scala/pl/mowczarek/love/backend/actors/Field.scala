package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.pattern.ask
import akka.util.Timeout
import pl.mowczarek.love.backend.actors.CreatureActor.Accost
import pl.mowczarek.love.backend.actors.Field._
import pl.mowczarek.love.backend.actors.SystemMap.GetRandomField
import pl.mowczarek.love.backend.model.Creature

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by neo on 22.03.17.
  */
class Field(x: Int, y: Int, gameMap: ActorRef, sinkActor: ActorRef) extends Actor with ActorLogging {

  private var creatures: Map[ActorRef, Creature] = Map()

  implicit val timeout: Timeout = 5 seconds

  override def preStart = {
    log.info(s"Field created: x: $x, y: $y")
  }

  override def receive: Receive = {
    case c@SpawnCreature(creature) =>
      creatures += sender -> creature
      context.watch(sender)
      log.info(s"Creature landed on field ($x, $y)")
      forwardToSocket(c.toEvent(x,y))

    case command @ Accost(attributes) =>
      creatures.keys.filter(_ != sender).foreach { creatureRef =>
        creatureRef forward command
      }

    case c@Emigrate(creature) =>
      val creatureRef = sender
      (gameMap ? GetRandomField(x, y)).mapTo[ActorRef].onComplete {
        case Failure(ex) => throw ex
        case Success(field: ActorRef) =>
          creatures -= sender
          context.unwatch(sender)
          log.info(s"Creature left field ($x, $y)")
          creatureRef ! field
          field forward Immigrate(creature)
          forwardToSocket(c.toEvent(x,y))
      }

    case c@Immigrate(creature) =>
      creatures += sender -> creature
      context.watch(sender)
      log.info(s"Creature migrated onto field ($x, $y)")
      forwardToSocket(c.toEvent(x,y))

    //This message comes from context.watch akka mechanism
    case Terminated(subject: ActorRef) =>
      creatures -= subject
      creatures.get(subject).foreach { creature =>
        forwardToSocket(CreatureDied(creature, x, y))
      }

  }

  private def forwardToSocket(e: FieldEvent) = sinkActor ! e

}

object Field {

  sealed trait FieldCommand extends ActorCommand
  case class SpawnCreature(creature: Creature) extends FieldCommand {
    def toEvent(x: Int, y: Int) = CreatureSpawned(creature, x, y)
  }
  case class Emigrate(creature: Creature) extends FieldCommand {
    def toEvent(x: Int, y: Int) = CreatureEmigrated(creature, x, y)
  }
  case class Immigrate(creature: Creature) extends FieldCommand {
    def toEvent(x: Int, y: Int) = CreatureImmigrated(creature, x, y)
  }

  sealed trait FieldEvent extends ActorEvent
  case class CreatureSpawned(creature: Creature, x: Int, y: Int) extends FieldEvent
  case class CreatureEmigrated(creature: Creature, x: Int, y: Int) extends FieldEvent
  case class CreatureImmigrated(creature: Creature, x: Int, y: Int) extends FieldEvent
  case class CreatureDied(creature: Creature, x: Int, y: Int) extends FieldEvent

  def props(x: Int, y: Int, gameMap: ActorRef, sinkActor: ActorRef) = Props(new Field(x, y, gameMap, sinkActor))

}
