package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import pl.mowczarek.love.backend.actors.CreatureManager.KillAllCreatures
import pl.mowczarek.love.backend.actors.SystemMap.{GetField, GetRandomField}
import pl.mowczarek.love.backend.config.Config

import scala.util.Random

/**
  * Created by neo on 22.03.17.
  */
class SystemMap(sinkActor: ActorRef, system: ActorSystem) extends Actor {

  private var fieldsMap: Map[Coordinates, ActorRef] = Map()

  val mapSize = Config.mapSize

  override def preStart = {
    for {
      x <- 1 to mapSize
      y <- 1 to mapSize
    } yield {
      val newField = system.actorOf(Field.props(x, y, self, sinkActor))
      fieldsMap += (Coordinates(x, y) -> newField)
    }
  }

  override def receive: Receive = {
    case GetRandomField =>
      fieldsMap.get(Coordinates(Random.nextInt(mapSize)+1, Random.nextInt(mapSize)+1)).foreach { field =>
        sender ! field
      }

    case GetRandomField(x, y) =>
      val potentialField = fieldsMap.get(Coordinates(x + Random.nextInt(3)-1, y + Random.nextInt(3)-1))
      potentialField.orElse(fieldsMap.get(Coordinates(x, y))).foreach(field => sender ! field)

    case KillAllCreatures =>
      fieldsMap.values.foreach(_ ! KillAllCreatures)

    case GetField(coordinates) =>
      fieldsMap.get(coordinates).foreach { field =>
        sender ! field
      }
  }
}

object SystemMap {

  case class GetField(coordinates: Coordinates)

  case object GetRandomField

  case class GetRandomField(x: Int, y: Int)

  def props(sinkActor: ActorRef)(implicit system: ActorSystem) = Props(new SystemMap(sinkActor, system))
}

case class Coordinates(x: Int, y: Int)
