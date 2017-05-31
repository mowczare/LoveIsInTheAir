package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import pl.mowczarek.love.backend.actors.CreatureManager.{AddCreature, AddRandomCreature, KillAllCreatures, StartGame}
import pl.mowczarek.love.backend.config.Config
import akka.pattern.ask
import akka.util.Timeout
import pl.mowczarek.love.backend.actors.Field.SpawnCreature
import pl.mowczarek.love.backend.model.Creature

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by neo on 22.03.17.
  */
class CreatureManager extends Actor {

  import Paths._

  implicit val timeout: Timeout = 5 seconds

  implicit val system = context.system

  override def receive: Receive = {
    case StartGame =>
      (1 to Config.creaturesAtStart).foreach(_ => self ! AddRandomCreature)

    case AddRandomCreature =>
      fieldsPath ! SpawnCreature(Creature.generate(), Coordinates.random)

    case AddCreature(creature) =>
      fieldsPath ! SpawnCreature(creature, Coordinates.random)

    case KillAllCreatures =>
      systemMap ! KillAllCreatures
  }
}

object CreatureManager {

  case object StartGame

  case object AddRandomCreature

  case class AddCreature(creature: Creature)

  case object KillAllCreatures

  def props = Props(new CreatureManager)

  def clusterSingletonProps(implicit system: ActorSystem): Props =
    ClusterSingletonManager.props(
      singletonProps = props,
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system))
}