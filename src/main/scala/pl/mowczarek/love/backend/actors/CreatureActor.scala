package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import pl.mowczarek.love.backend.actors.CreatureActor._
import pl.mowczarek.love.backend.actors.Field.{Accost, Emigrate, MatureCreature, SpawnCreature}
import pl.mowczarek.love.backend.model.{Attributes, Creature, Male, Sex}
import akka.pattern.ask
import akka.util.Timeout

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
    //TODO move adolescence time and tryToAccost time to config
    context.system.scheduler.scheduleOnce(15 seconds, self, Mature)
    context.system.scheduler.scheduleOnce(Random.nextInt(30)+50 seconds, self, Die)
    context.system.scheduler.schedule(Random.nextInt(15) + 10 seconds, Random.nextInt(15) + 10 seconds, self, Migrate)
  }

  override def receive: Receive = {
    case Die =>
      self ! PoisonPill
      log.info("Creature is dead")
    case Mature =>
      context.system.scheduler.schedule(2 seconds, 2 seconds, self, TryToAccost)
      log.info("Creature is mature now")
      context.become(mature)
      fieldsPath ! MatureCreature(thisCreature, thisField)
  }

  def mature: Receive = {
    case Die =>
      self ! PoisonPill
      log.info("Creature is dead")

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
      context.system.scheduler.schedule(2 seconds, 2 seconds, self, Copulate)
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
      if (Random.nextInt(100) < 20) {
        log.info("Pair is having a baby")
        context.system.scheduler.scheduleOnce(1 second, self, Reproduce)
        context.become(pregnant)
      }

    case Die =>
      self ! PoisonPill
      log.info("Creature is dead")
  }

  def pregnant: Receive = {
    case Reproduce =>
      pairedCreature.foreach { creature =>
        log.info("Reproducing")
        context.actorOf(CreatureActor.props(thisField, thisCreature.mixWith(creature)))
      }
      context.system.scheduler.schedule(2 seconds, 2 seconds, self, Copulate)
      context.become(postPair)

    case Die =>
      self ! PoisonPill
      log.info("Creature is dead")
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
