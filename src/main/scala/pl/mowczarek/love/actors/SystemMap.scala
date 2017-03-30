package pl.mowczarek.love.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import pl.mowczarek.love.actors.SystemMap.GetRandomField
import pl.mowczarek.love.config.Config

import scala.util.Random

/**
  * Created by neo on 22.03.17.
  */
class SystemMap(system: ActorSystem) extends Actor {

  private var fieldsMap: Map[Coordinates, ActorRef] = Map()

  val mapSize = Config.mapSize

  override def preStart = {
    for {
      x <- 1 to mapSize
      y <- 1 to mapSize
    } yield {
      val newField = system.actorOf(Field.props(x, y, self))
      fieldsMap += (Coordinates(x, y) -> newField)
    }
  }

  override def receive: Receive = {
    case GetRandomField =>
      fieldsMap.get(Coordinates(Random.nextInt(mapSize)+1, Random.nextInt(mapSize)+1)).foreach { field =>
        sender ! field
      }
  }
}

object SystemMap {

  case object GetRandomField

  def props(implicit system: ActorSystem) = Props(new SystemMap(system))
}

case class Coordinates(x: Int, y: Int)
