package pl.mowczarek.love.backend.model

import java.util.UUID

import scala.util.Random

/**
  * Created by neo on 15.03.17.
  */

case class Creature(attributes: Attributes,
                    preferences: Attributes,
                    sex: Sex,
                    desperation: Double,
                    id: String) {

  def isAttractedTo(otherAttributes: Attributes): Boolean = {
    preferences.areMatchedWith(otherAttributes, desperation)
  }

  def increaseDesperation = copy(desperation = this.desperation + 0.1)

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
               desperation: Double = 1.0,
               id: String = UUID.randomUUID().toString): Creature = {
    Creature(attributes, preferences, sex, desperation, id)
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
