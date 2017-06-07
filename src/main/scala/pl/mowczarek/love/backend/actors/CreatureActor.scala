package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import pl.mowczarek.love.backend.actors.CreatureActor._
import pl.mowczarek.love.backend.actors.Field._
import pl.mowczarek.love.backend.model.{Attributes, Creature, Male, Sex}
import akka.pattern.ask
import akka.util.Timeout
import pl.mowczarek.love.backend.config.Config

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}

/**
  * Created by neo on 15.03.17.
  */
class CreatureActor(thisCreatureInitialState: Creature, fieldCoordinates: Coordinates) extends Actor
  with ActorLogging {

  import Paths._

  implicit val timeout: Timeout = 5 seconds

  implicit val system = context.system

  private var thisField = fieldCoordinates
  private var thisCreature = thisCreatureInitialState
  private var pairedCreature: Option[Creature] = None

  override def preStart = {
    log.info(s"${this.thisCreature} created in baby state")
    context.system.scheduler.scheduleOnce(Config.matureTime seconds, self, Mature)
    context.system.scheduler.scheduleOnce(Random.nextInt(Config.lifeTimeDelta)+ Config.lifeTime seconds, self, Die)
    context.system.scheduler.schedule(
      Random.nextInt(Config.firstMigrationTimeDelta) + Config.firstMigrationTime seconds,
      Random.nextInt(Config.nextMigrationsTimeDelta) + Config.nextMigrationsTime seconds,
      self, Migrate
    )
  }

  override def receive: Receive = {
    case Die =>
      fieldsPath ! CreatureDied(thisCreature.copy(state = "dead"), thisField.x, thisField.y)
      self ! PoisonPill
      log.info("Creature is dead" + thisCreature)
    case Mature =>
      context.system.scheduler.schedule(Config.accostingFrequency seconds, Config.accostingFrequency seconds,
        self, TryToAccost)
      log.info("Creature is mature now")
      context.become(mature)
      fieldsPath ! MatureCreature(thisCreature, thisField)
  }

  def mature: Receive = {
    case Die =>
      fieldsPath ! CreatureDied(thisCreature.copy(state = "dead"), thisField.x, thisField.y)
      self ! PoisonPill
      log.info("Creature is dead" + thisCreature.id)


    case TryToAccost =>
      fieldsPath ! Accost(thisCreature.attributes, thisCreature.sex, thisField)

    case Accost(attributes, sex, _) =>
      if (thisCreature.isAttractedTo(attributes, sex))
        sender ! Interest(thisCreature)
      else {
        thisCreature = thisCreature.increaseDesperation
        log.info(s"First creature not attracted")
      }

    case Interest(otherCreature) =>
      if (thisCreature.isAttractedTo(otherCreature.attributes, otherCreature.sex)) {
        pair(otherCreature)
        sender ! Match(thisCreature)
        context.become(postPair)
      } else log.info("Second creature not attracted")

    case Match(otherCreature) =>
      log.info(s"Paired creature $thisCreature with $otherCreature")
      pair(otherCreature)
      context.system.scheduler.schedule(Config.copulationFrequency seconds, Config.copulationFrequency seconds,
        self, Copulate)
      context.become(postPair)

    case Migrate =>
      (fieldsPath ? Emigrate(thisCreature, thisField)).mapTo[Coordinates].onComplete {
        case Success(ref) => thisField = ref
        case Failure(ex) => throw ex
      }
      log.info("Creature is migrating")
  }

  def postPair: Receive = {
    case Copulate =>
      log.info("Trying to copulate")
      //TODO move copulate percentage to config
      if (Random.nextInt(100) < Config.chanceOfReproducingWhileCopulating) {
        log.info("Pair is having a baby")
        context.system.scheduler.scheduleOnce(Config.pregnancyTime seconds, self, Reproduce)
        context.become(pregnant)
      }

    case Die =>
      fieldsPath ! CreatureDied(thisCreature.copy(state = "dead"), thisField.x, thisField.y)
      self ! PoisonPill
      log.info("Creature is dead" + thisCreature.id)
  }

  def pregnant: Receive = {
    case Reproduce =>
      pairedCreature.foreach { creature =>
        log.info("Reproducing")
        fieldsPath ! SpawnCreature(thisCreature.mixWith(creature), thisField)
      }
      context.system.scheduler.schedule(Config.copulationFrequency seconds, Config.copulationFrequency seconds,
        self, Copulate)
      context.become(postPair)

    case Die =>
      fieldsPath ! CreatureDied(thisCreature.copy(state = "dead"), thisField.x, thisField.y)
      self ! PoisonPill
      log.info("Creature is dead" + thisCreature.id)
  }

  private def pair(otherCreature: Creature) = {
    pairedCreature = Some(otherCreature)
  }
}

object CreatureActor {

  sealed trait CreatureCommand extends ActorCommand
  case object Mature extends CreatureCommand
  case object TryToAccost extends CreatureCommand
  case class Interest(otherCreature: Creature) extends CreatureCommand
  case class Match(otherCreature: Creature) extends CreatureCommand
  case object Copulate extends CreatureCommand
  case object Reproduce extends CreatureCommand
  case object Die extends CreatureCommand
  case object Migrate extends CreatureCommand

  def props(field: Coordinates): Props = props(field, Creature.generate())
  def props(field: Coordinates, creature: Creature): Props =
    Props(new CreatureActor(creature, field))
}
