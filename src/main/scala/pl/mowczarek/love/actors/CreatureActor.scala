package pl.mowczarek.love.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import pl.mowczarek.love.actors.CreatureActor.{Accost, Interest, Match, TryToAccost}
import pl.mowczarek.love.actors.Field.SpawnCreature
import pl.mowczarek.love.model.{Attributes, Creature}

import scala.concurrent.duration._
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by neo on 15.03.17.
  */
class CreatureActor(thisCreatureInitialState: Creature, field: ActorRef, system: ActorSystem)
  extends Actor
    with ActorLogging {

  private var thisCreature = thisCreatureInitialState
  private var pairedCreature: Option[Creature] = None

  override def preStart = {
    log.info(s"${this.thisCreature} created")
    field ! SpawnCreature
    system.scheduler.schedule(2 seconds, 2 seconds, self, TryToAccost)
  }

  override def receive: Receive = {
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
      } else log.info("Second creature not attracted")

    case Match(otherCreature) =>
      log.info(s"Paired creature $thisCreature with $otherCreature")
      pair(otherCreature)
      context.become(postPair)
  }

  def postPair: Receive = {
    case _ =>
  }

  private def pair(otherCreature: Creature) = {
    pairedCreature = Some(otherCreature)
  }
}

object CreatureActor {
  sealed trait CreatureCommand
  case object TryToAccost extends CreatureCommand
  case class Accost(attributes: Attributes) extends CreatureCommand
  case class Interest(otherCreature: Creature) extends CreatureCommand
  case class Match(otherCreature: Creature) extends CreatureCommand

  def props(field: ActorRef)(implicit system: ActorSystem): Props = props(field, Creature())
  def props(field: ActorRef, creature: Creature)(implicit system: ActorSystem): Props =
    Props(new CreatureActor(creature, field, system))
}
