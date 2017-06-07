package pl.mowczarek.love.backend.model

import java.util.UUID

import pl.mowczarek.love.backend.config.Config

import scala.util.Random

/**
  * Created by neo on 15.03.17.
  */

case class Creature(attributes: Attributes,
                    preferences: Attributes,
                    sex: Sex,
                    desperation: Double,
                    id: String,
                    state: String) {

  def isAttractedTo(otherAttributes: Attributes, sex: Sex): Boolean = {
    //Please do not sue me for being homophobic, creatures of the same sex cannot reproduce, it is for simplicity sake.
    this.preferences.areMatchedWith(otherAttributes, desperation) && this.sex != sex
  }

  def increaseDesperation = copy(desperation = this.desperation + Config.desperationIncreaseFactor)

  def mixWith(otherCreature: Creature) = {
    Creature.generate(
      attributes.mixWith(otherCreature.attributes),
      preferences.mixWith(otherCreature.preferences),
      sex = Sex.random)
  }
}

object Creature {
  def generate(attributes: Attributes = Attributes.random,
               preferences: Attributes = Attributes.random,
               sex: Sex = Sex.random,
               desperation: Double = Config.initialDesperation,
               id: String = UUID.randomUUID().toString,
               state: String = "spawned"): Creature = {
    Creature(attributes, preferences, sex, desperation, id, state)
  }
}

sealed trait Sex {
  def canReproduceWith(sex: Sex): Boolean = this != sex
}
case object Male extends Sex
case object Female extends Sex

object Sex {
  def random = if (Random.nextBoolean) Male else Female
  def apply(sex: String) = {
    sex.toLowerCase match {
      case "male" => Male
      case "female" => Female
    }
  }
}
