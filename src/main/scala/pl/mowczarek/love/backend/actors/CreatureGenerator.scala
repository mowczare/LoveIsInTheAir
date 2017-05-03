package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import pl.mowczarek.love.backend.actors.CreatureGenerator.StartGame
import pl.mowczarek.love.backend.config.Config
import akka.pattern.ask
import akka.util.Timeout
import pl.mowczarek.love.backend.actors.SystemMap.GetRandomField

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by neo on 22.03.17.
  */
class CreatureGenerator(systemMap: ActorRef, implicit val system: ActorSystem) extends Actor {

  implicit val timeout: Timeout = 5 seconds

  override def receive: Receive = {
    case StartGame =>
      (1 to Config.creaturesAtStart) foreach { _ =>
        (systemMap ? GetRandomField).mapTo[ActorRef].onComplete {
          case Failure(ex) => throw ex
          case Success(field: ActorRef) =>
            system.actorOf(CreatureActor.props(field))
        }
      }
  }
}

object CreatureGenerator {

  case object StartGame

  def props(systemMap: ActorRef)(implicit system: ActorSystem) = Props(new CreatureGenerator(systemMap, system))
}