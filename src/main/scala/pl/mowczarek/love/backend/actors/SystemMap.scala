package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Kill, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import pl.mowczarek.love.backend.actors.CreatureManager.KillAllCreatures
import pl.mowczarek.love.backend.actors.Field.{CreateField, GetFieldStatus}
import pl.mowczarek.love.backend.actors.SystemMap.GetMapStatus
import pl.mowczarek.love.backend.config.Config
import pl.mowczarek.love.backend.model.Creature

import scala.util.{Failure, Random, Success}
import akka.pattern.ask
import akka.util.Timeout
import pl.mowczarek.love.backend.socket.DispatcherActor

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by neo on 22.03.17.
  */
class SystemMap extends Actor {

  import Paths._

  private var fieldsSet: Set[Coordinates] = Set()

  val mapSize = Config.mapSize

  implicit val system = context.system

  implicit val timeout: Timeout = 15 seconds

  override def preStart = {
    for {
      x <- 0 until Config.mapSize
      y <- 0 until Config.mapSize
    } yield {
      context.system.scheduler.scheduleOnce(20 seconds, fieldsPath, CreateField(Coordinates(x,y)))
      fieldsSet = fieldsSet + Coordinates(x,y)
    }
  }

  override def receive: Receive = {

    case KillAllCreatures =>
      fieldsSet.foreach(coords => fieldsPath ! Field.KillAllCreatures(coords))

    case GetMapStatus =>
      val restSender = sender()
      val stateFutures = fieldsSet.map { coords =>
        (fieldsPath ? GetFieldStatus(coords)).mapTo[List[Creature]].map(creatures => FieldState(coords, creatures))
      }.toList
      Future.sequence(stateFutures).onComplete {
        case Success(fieldStates) =>
          restSender ! MapState(Config.mapSize, Config.mapSize, fieldStates)
        case Failure(ex) => throw ex
      }
  }
}

object SystemMap {

  case object GetMapStatus

  def props: Props = Props(new SystemMap)

  def clusterSingletonProps(implicit system: ActorSystem): Props =
    ClusterSingletonManager.props(
    singletonProps = props,
    terminationMessage = PoisonPill,
    settings = ClusterSingletonManagerSettings(system))
}

case class FieldState(coordinates: Coordinates, creatures: List[Creature])

case class MapState(sizeX: Int, sizeY: Int, states: List[FieldState])

case class Coordinates(x: Int, y: Int) {
  def neighbours: List[Coordinates] =
    List(Coordinates(x-1, y-1), Coordinates(x-1, y), Coordinates(x-1, y+1), Coordinates(x, y-1),
      Coordinates(x, y+1), Coordinates(x+1, y-1), Coordinates(x+1, y), Coordinates(x+1, y+1))
      .filter(coords => coords.x >= 0 && coords.y >= 0 && coords.y < Config.mapSize && coords.x < Config.mapSize)

  def randomNeighbor: Coordinates = Random.shuffle(neighbours).head
}

object Coordinates {
  def random = Coordinates(Random.nextInt(Config.mapSize), Random.nextInt(Config.mapSize))
}

