package pl.mowczarek.love.backend.model

import pl.mowczarek.love.backend.config.Config

import scala.language.postfixOps
import scala.util.Random

/**
  * Created by neo on 15.03.17.
  */

case class Attributes(attributes: Map[String, Attribute]) {

  private val totalValue = attributes.values.map(at => at.value * at.importance).sum

  def areMatchedWith(otherAttributes: Attributes, desperation: Double): Boolean = {
    val points = attributes.map { case (name, myAttribute) =>
      otherAttributes.attributes.get(name).map(_.value).getOrElse(0) * myAttribute.importance
    }.sum * desperation
    points > totalValue * Config.initialFuss
  }

  def mixWith(otherAttributes: Attributes): Attributes = {
    val newAttributes = attributes.values zip otherAttributes.attributes.values map {
      case (attr1: Attribute, attr2: Attribute) =>
        val newValue = (attr1.value + attr2.value) / 2
        val newImportance = (attr1.importance + attr2.importance) / 2
        attr1.name -> Attribute(attr1.name, newValue, newImportance)
    } toMap

    Attributes(newAttributes)
  }
}

object Attributes {
  val names = Seq("a", "b", "c", "d", "e", "f", "g", "h", "i", "j") //todo add real names
  def random: Attributes = random(10)
  def random(numberOfAttributes: Int) : Attributes = {
    val attributes = (1 to numberOfAttributes).map { _ =>
      val newAttribute = Attribute.random
      newAttribute.name -> newAttribute
    }.toMap
    Attributes(attributes)
  }
}

case class Attribute(name: String, value: Int, importance: Int)

object Attribute {
  def random: Attribute = Attribute(Attributes.names.drop(Random.nextInt(Attributes.names.length)).head, Random.nextInt(10), Random.nextInt(10))
}