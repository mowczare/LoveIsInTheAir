package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.util.Timeout
import pl.mowczarek.love.backend.actors.CreatureActor.Die
import pl.mowczarek.love.backend.actors.Field._
import pl.mowczarek.love.backend.config.Config
import pl.mowczarek.love.backend.model.{Attributes, Creature, Sex}
import upickle.default._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by neo on 22.03.17.
  */
class Field extends Actor with ActorLogging {

  import Paths._

  private var creatures: Map[ActorRef, Creature] = Map()

  implicit val system = context.system

  implicit val timeout: Timeout = 5 seconds

  override def receive: Receive = {
    case CreateField(coordinates: Coordinates) =>
      log.info(s"Field created: x: ${coordinates.x}, y: ${coordinates.y}")
      context.become(postCreate(coordinates.x, coordinates.y))

    case msg => log.error("Got Field command not in created state: {}", msg)
  }

  def postCreate(x: Int, y: Int): Receive = {

    case KillAllCreatures(_) =>
      creatures.keys.foreach(_ ! Die)

    case c@SpawnCreature(creature, _) =>
      val newCreature = context.actorOf(CreatureActor.props(Coordinates(x, y), creature))
      creatures += newCreature -> creature
      context.watch(newCreature)
      log.info(s"Creature landed on field ($x, $y)")
      forwardToSocket(write(c.toEvent))

    case command @ Accost(attributes, sex, _) =>
      creatures.keys.filter(_ != sender).foreach { creatureRef =>
        creatureRef forward command
      }

    case c@Emigrate(creature, coordinates) =>
      val creatureRef = sender
      creature.state = "emigrated"
      forwardToSocket(write(c.toEvent))
      val randomNeighbour = Coordinates(x, y).randomNeighbor
      creatures -= creatureRef
      context.unwatch(creatureRef)
      log.info(s"Creature left field ($x, $y)")
      creatureRef ! randomNeighbour
      fieldsPath ! Immigrate(creature, creatureRef, randomNeighbour)

    case c@Immigrate(creature, creatureRef, _) =>
      creature.state = "immigrated"
      creatures += creatureRef -> creature
      context.watch(creatureRef)
      log.info(s"Creature migrated onto field ($x, $y)")
      forwardToSocket(write(c.toEvent))

    case c@MatureCreature(creature, _) =>
      creature.state = "mature"
      log.info(s"Creature matured on field ($x, $y)")
      forwardToSocket(write(c.toEvent))

    case GetFieldStatus(_) =>
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

  sealed trait FieldCommand extends ActorCommand {
    def coordinates: Coordinates
  }

  case class SpawnCreature(creature: Creature, coordinates: Coordinates) extends FieldCommand {
    def toEvent = CreatureSpawned(creature, coordinates.x, coordinates.y)
  }
  case class MatureCreature(creature: Creature, coordinates: Coordinates) extends FieldCommand {
    def toEvent = CreatureMature(creature, coordinates.x, coordinates.y)
  }
  case class Emigrate(creature: Creature, coordinates: Coordinates) extends FieldCommand {
    def toEvent = CreatureEmigrated(creature, coordinates.x, coordinates.y)
  }
  case class Immigrate(creature: Creature, creatureRef: ActorRef, coordinates: Coordinates) extends FieldCommand {
    def toEvent = CreatureImmigrated(creature, coordinates.x, coordinates.y)
  }
  case class CreateField(coordinates: Coordinates) extends FieldCommand {
    def toEvent = FieldCreated(coordinates.x, coordinates.y)
  }
  case class Accost(attributes: Attributes, sex: Sex, coordinates: Coordinates) extends FieldCommand {
    def toEvent = Accosted(attributes, sex, coordinates.x, coordinates.y)
  }
  case class KillAllCreatures(coordinates: Coordinates) extends FieldCommand {
    def toEvent = AllCreaturesKilled(coordinates.x, coordinates.y)
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
  case class Accosted(attributes: Attributes, sex: Sex, x: Int, y: Int) extends FieldEvent
  case class AllCreaturesKilled(x: Int, y: Int) extends FieldEvent

  sealed trait FieldMessage {
    def coordinates: Coordinates
  }

  case class GetFieldStatus(coordinates: Coordinates) extends FieldMessage

  def props = Props(new Field)

  def clusterShardingProps(implicit actorSystem: ActorSystem) =
    ClusterSharding(actorSystem).start(
      typeName = "Field",
      entityProps = props,
      settings = ClusterShardingSettings(actorSystem),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )

  private val extractEntityId: ShardRegion.ExtractEntityId = {
    case a: FieldCommand => (s"${a.coordinates.x},${a.coordinates.y}", a)
    case a: FieldMessage => (s"${a.coordinates.x},${a.coordinates.y}", a)
    case a: FieldEvent => (s"${a.x}x${a.y}y", a)
  }

  private val extractShardId: ShardRegion.ExtractShardId = {
    case a: FieldCommand => (s"${a.coordinates.x},${a.coordinates.y}".hashCode % Config.numberOfShards).toString
    case a: FieldMessage => (s"${a.coordinates.x},${a.coordinates.y}".hashCode % Config.numberOfShards).toString
    case a: FieldEvent => (s"${a.x}x${a.y}y".hashCode % Config.numberOfShards).toString
  }

}
