package pl.mowczarek.love.actors

import akka.actor.{Actor, Props}
import pl.mowczarek.love.actors.CreatureActor.{Accost, Interest, Match}
import pl.mowczarek.love.model.{Attributes, Creature}

/**
  * Created by neo on 15.03.17.
  */
class CreatureActor(thisCreature: Creature) extends Actor {

  private var pairedCreature: Option[Creature] = None

  //TODO Add second receive when creature is paired

  override def receive: Receive = {
    case Accost(attributes) =>
      if (thisCreature.isAttractedTo(attributes))
        sender ! Interest(thisCreature)

    case Interest(otherCreature) =>
      if (thisCreature.isAttractedTo(otherCreature.attributes)) {
        pair(otherCreature)
        sender ! Match(thisCreature)
      }

    case Match(otherCreature) =>
      pair(otherCreature)
  }

  private def pair(otherCreature: Creature) = {
    pairedCreature = Some(otherCreature)
  }
}

object CreatureActor {
  sealed trait CreatureCommand
  case class Accost(attributes: Attributes) extends CreatureCommand
  case class Interest(otherCreature: Creature) extends CreatureCommand
  case class Match(otherCreature: Creature) extends CreatureCommand

  def props = Props(new CreatureActor(Creature()))
  def props(creature: Creature) = Props(new CreatureActor(creature))
}
