package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import pl.mowczarek.love.backend.actors.CreatureManager.{AddCreature, AddRandomCreature, KillAllCreatures, StartGame}
import pl.mowczarek.love.backend.config.Config
import akka.pattern.ask
import akka.util.Timeout
import pl.mowczarek.love.backend.actors.SystemMap.{GetField, GetRandomField}
import pl.mowczarek.love.backend.model.Creature

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by neo on 22.03.17.
  */
class CreatureManager(systemMap: ActorRef) extends Actor {

  implicit val timeout: Timeout = 5 seconds

  override def receive: Receive = {
    case StartGame =>
      (1 to Config.creaturesAtStart).foreach(_ => self ! AddRandomCreature)

    case AddRandomCreature =>
      (systemMap ? GetRandomField).mapTo[ActorRef].onComplete {
        case Failure(ex) => throw ex
        case Success(field: ActorRef) =>
          context.actorOf(CreatureActor.props(field))
      }

    case AddCreature(creature) =>
      (systemMap ? GetRandomField).mapTo[ActorRef].onComplete {
        case Failure(ex) => throw ex
        case Success(field: ActorRef) =>
          context.actorOf(CreatureActor.props(field, creature))
      }

    case KillAllCreatures =>
      systemMap ! KillAllCreatures
  }
}

object CreatureManager {

  case object StartGame

  case object AddRandomCreature

  case class AddCreature(creature: Creature)

  case object KillAllCreatures

  def props(systemMap: ActorRef) = Props(new CreatureManager(systemMap))
}