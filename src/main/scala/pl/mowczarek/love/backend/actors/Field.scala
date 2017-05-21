package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.ask
import akka.util.Timeout
import pl.mowczarek.love.backend.actors.CreatureActor.{Accost, Die}
import pl.mowczarek.love.backend.actors.CreatureManager.KillAllCreatures
import pl.mowczarek.love.backend.actors.Field._
import pl.mowczarek.love.backend.actors.SystemMap.GetRandomField
import pl.mowczarek.love.backend.config.Config
import pl.mowczarek.love.backend.model.Creature
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by neo on 22.03.17.
  */
class Field(gameMap: ActorRef, sinkActor: ActorRef) extends Actor with ActorLogging {

  private var creatures: Map[ActorRef, Creature] = Map()

  implicit val timeout: Timeout = 5 seconds

  override def receive: Receive = {
    case CreateField(x: Int, y: Int) =>
      log.info(s"Field created: x: $x, y: $y")
      context.become(postCreate(x, y))

    case msg => log.error("Got Field command not in created state: {}", msg)
  }

  def postCreate(x: Int, y: Int): Receive = {

    case KillAllCreatures =>
      creatures.keys.foreach(_ ! Die)

    case c@SpawnCreature(creature) =>
      creatures += sender -> creature
      context.watch(sender)
      log.info(s"Creature landed on field ($x, $y)")
      forwardToSocket(write(c.toEvent(x,y)))

    case command @ Accost(attributes, sex) =>
      creatures.keys.filter(_ != sender).foreach { creatureRef =>
        creatureRef forward command
      }

    case c@Emigrate(creature) =>
      val creatureRef = sender
      creature.state = "emigrated"
      forwardToSocket(write(c.toEvent(x,y)))
      (gameMap ? GetRandomField(x, y)).mapTo[ActorRef].onComplete {
        case Failure(ex) => throw ex
        case Success(field: ActorRef) =>
          creatures -= creatureRef
          context.unwatch(creatureRef)
          log.info(s"Creature left field ($x, $y)")
          creatureRef ! field
          field ! Immigrate(creature, creatureRef)
      }

    case c@Immigrate(creature, creatureRef) =>
      creature.state = "immigrated"
      creatures += creatureRef -> creature
      context.watch(creatureRef)
      log.info(s"Creature migrated onto field ($x, $y)")
      forwardToSocket(write(c.toEvent(x,y)))

    case c@MatureCreature(creature) =>
      creature.state = "mature"
      log.info(s"Creature matured on field ($x, $y)")
      forwardToSocket(write(c.toEvent(x,y)))

    case GetFieldStatus =>
      sender ! creatures.values.toList

    //This message comes from context.watch akka mechanism
    case Terminated(subject: ActorRef) =>
      creatures.get(subject).foreach { creature =>
        creature.state = "dead"
        forwardToSocket(write(CreatureDied(creature, x, y)))
      }
      creatures -= subject
  }


  private def forwardToSocket(e: String) = sinkActor ! e

}

object Field {

  sealed trait FieldCommand extends ActorCommand
  case class SpawnCreature(creature: Creature) extends FieldCommand {
    def toEvent(x: Int, y: Int) = CreatureSpawned(creature, x, y)
  }
  case class MatureCreature(creature: Creature) extends FieldCommand {
    def toEvent(x: Int, y: Int) = CreatureMature(creature, x, y)
  }
  case class Emigrate(creature: Creature) extends FieldCommand {
    def toEvent(x: Int, y: Int) = CreatureEmigrated(creature, x, y)
  }
  case class Immigrate(creature: Creature, creatureRef: ActorRef) extends FieldCommand {
    def toEvent(x: Int, y: Int) = CreatureImmigrated(creature, x, y)
  }
  case class CreateField(x: Int, y: Int) extends FieldCommand {
    def toEvent(x: Int, y: Int) = FieldCreated(x, y)
  }

  sealed trait FieldEvent extends ActorEvent {
    def x: Int
    def y: Int
  }

  case class CreatureSpawned(creature: Creature, x: Int, y: Int) extends FieldEvent
  case class CreatureMature(creature: Creature, x: Int, y: Int) extends FieldEvent
  case class CreatureEmigrated(creature: Creature, x: Int, y: Int) extends FieldEvent
  case class CreatureImmigrated(creature: Creature, x: Int, y: Int) extends FieldEvent
  case class CreatureDied(creature: Creature, x: Int, y: Int) extends FieldEvent
  case class FieldCreated(x: Int, y: Int) extends FieldEvent

  case object GetFieldStatus

  def props(gameMap: ActorRef, sinkActor: ActorRef) = Props(new Field(gameMap, sinkActor))

  def clusterShardingProps(gameMap: ActorRef, sinkActor: ActorRef)(implicit actorSystem: ActorSystem) =
    ClusterSharding(actorSystem).start(
      typeName = "Field",
      entityProps = props(gameMap, sinkActor),
      settings = ClusterShardingSettings(actorSystem),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )

  private val extractEntityId: ShardRegion.ExtractEntityId = {
    case a: FieldEvent => (s"${a.x},${a.y}", a)
  }

  private val extractShardId: ShardRegion.ExtractShardId = {
    case a: FieldEvent => (s"${a.x},${a.y}".hashCode % Config.numberOfShards).toString
  }

}
