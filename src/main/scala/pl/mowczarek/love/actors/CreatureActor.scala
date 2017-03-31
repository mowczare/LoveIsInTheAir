package pl.mowczarek.love.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import pl.mowczarek.love.actors.CreatureActor._
import pl.mowczarek.love.actors.Field.SpawnCreature
import pl.mowczarek.love.model.Sex.Male
import pl.mowczarek.love.model.{Attributes, Creature}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

/**
  * Created by neo on 15.03.17.
  */
class CreatureActor(thisCreatureInitialState: Creature, field: ActorRef, implicit val system: ActorSystem)
  extends Actor
    with ActorLogging {

  private var thisCreature = thisCreatureInitialState
  private var pairedCreature: Option[Creature] = None

  override def preStart = {
    log.info(s"${this.thisCreature} created in baby state")
    field ! SpawnCreature
    //TODO move adolescence time and tryToAccost time to config
    system.scheduler.scheduleOnce(15 seconds, self, Mature)
    system.scheduler.scheduleOnce(Random.nextInt(30)+50 seconds, self, Die)
    system.scheduler.schedule(Random.nextInt(15) seconds, Random.nextInt(15) seconds, self, Migrate)
  }

  override def receive: Receive = {
    case Die =>
      self ! PoisonPill
      log.info("Creature is dead")
    case Mature =>
      if (thisCreature.sex == Male) system.scheduler.schedule(2 seconds, 2 seconds, self, TryToAccost)
      log.info("Creature is mature now")
      context.become(mature)
  }

  def mature: Receive = {
    case Die =>
      self ! PoisonPill
      log.info("Creature is dead")

    case TryToAccost =>
      field ! Accost(thisCreature.attributes)

    case Accost(attributes) =>
      if (thisCreature.isAttractedTo(attributes))
        sender ! Interest(thisCreature)
      else {
        thisCreature = thisCreature.increaseDesperation
        log.info(s"First creature not attracted")
      }

    case Interest(otherCreature) =>
      if (thisCreature.isAttractedTo(otherCreature.attributes)) {
        pair(otherCreature)
        sender ! Match(thisCreature)
        context.become(postPair)
      } else log.info("Second creature not attracted")

    case Match(otherCreature) =>
      log.info(s"Paired creature $thisCreature with $otherCreature")
      pair(otherCreature)
      system.scheduler.schedule(2 seconds, 2 seconds, self, Copulate)
      context.become(postPair)

    case Migrate =>
      field ! Migrate
      log.info("Creature is migrating")
  }

  def postPair: Receive = {
    case Copulate =>
      log.info("Trying to copulate")
      if (Random.nextInt(100) < 20) {
        log.info("Pair is having a baby")
        system.scheduler.scheduleOnce(1 second, self, Reproduce)
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
        system.actorOf(CreatureActor.props(field, thisCreature.mixWith(creature)))
      }

    case Die =>
      self ! PoisonPill
      log.info("Creature is dead")
  }

  private def pair(otherCreature: Creature) = {
    pairedCreature = Some(otherCreature)
  }
}

object CreatureActor {
  sealed trait CreatureCommand
  case object Mature extends CreatureCommand
  case object TryToAccost extends CreatureCommand
  case class Accost(attributes: Attributes) extends CreatureCommand
  case class Interest(otherCreature: Creature) extends CreatureCommand
  case class Match(otherCreature: Creature) extends CreatureCommand
  case object Copulate extends CreatureCommand
  case object Reproduce extends CreatureCommand
  case object Die extends CreatureCommand
  case object Migrate extends CreatureCommand


  def props(field: ActorRef)(implicit system: ActorSystem): Props = props(field, Creature())
  def props(field: ActorRef, creature: Creature)(implicit system: ActorSystem): Props =
    Props(new CreatureActor(creature, field, system))
}
