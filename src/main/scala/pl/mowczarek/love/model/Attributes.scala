package pl.mowczarek.love.model

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
    points > totalValue
  }
}

object Attributes {
  val names = Seq("a", "b", "c", "d", "e", "f", "g", "h", "i", "j") //todo add real names
  def random: Attributes = random(10) //todo move 10 to config
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