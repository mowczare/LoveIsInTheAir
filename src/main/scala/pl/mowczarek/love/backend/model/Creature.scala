package pl.mowczarek.love.backend.model

import java.util.UUID

import pl.mowczarek.love.backend.model.Sex.Sex

import scala.util.Random

/**
  * Created by neo on 15.03.17.
  */

case class Creature(attributes: Attributes = Attributes.random,
                    preferences: Attributes = Attributes.random,
                    sex: Sex = Sex.random,
                    desperation: Double = 1.0,
                    id: String = UUID.randomUUID().toString) {

  def isAttractedTo(otherAttributes: Attributes): Boolean = {
    preferences.areMatchedWith(otherAttributes, desperation)
  }

  def increaseDesperation = copy(desperation = this.desperation + 0.1)

  def mixWith(otherCreature: Creature) = {
    Creature(
      attributes.mixWith(otherCreature.attributes),
      preferences.mixWith(otherCreature.preferences),
      sex = Sex.random)
  }
}

object Sex {
  def random = if (Random.nextBoolean) Male else Female
  sealed trait Sex {
    def canReproduceWith(sex: Sex): Boolean = this != sex
  }
  case object Male extends Sex
  case object Female extends Sex
}
