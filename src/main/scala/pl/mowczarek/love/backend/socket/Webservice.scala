package pl.mowczarek.love.backend.socket

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import pl.mowczarek.love.backend.actors.{ActorEvent, MapState, Paths}
import pl.mowczarek.love.backend.actors.CreatureManager.{AddCreature, AddRandomCreature, KillAllCreatures}
import pl.mowczarek.love.backend.actors.SystemMap.GetMapStatus
import pl.mowczarek.love.backend.config.Config
import pl.mowczarek.love.backend.socket.DispatcherActor.ClientJoined

import scala.util.{Failure, Success}
import akka.pattern.ask
import akka.util.Timeout
import pl.mowczarek.love.backend.actors.Paths._
import upickle.default._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by neo on 03.04.17.
  */
class Webservice(fm: Materializer, system: ActorSystem) extends Directives {
  // TODO implement initialize backend endpoint
  import system.dispatcher

  implicit val materializer = fm
  implicit val actorSystem = system

  def route: Route =
    path("creature") {
      restRoutes
    } ~
    path("ws") {
      wsRoutes
    }

  implicit val timeout: Timeout = 15 seconds

  private val restRoutes: Route =
    pathEnd {
      delete {
        creatureManager ! KillAllCreatures
        respondWithHeaders(Webservice.defaultHeaders) {
          complete(StatusCodes.NoContent)
        }
      } ~
      post {
        creatureManager ! AddRandomCreature
        respondWithHeaders(Webservice.defaultHeaders) {
          complete(StatusCodes.Created)
        }
      } ~
      options {
        respondWithHeaders(Webservice.defaultHeaders) {
          complete(StatusCodes.OK)
        }
      } ~
      get {
        respondWithHeaders(Webservice.defaultHeaders) {
          onComplete((systemMap ? GetMapStatus).mapTo[MapState]) {
            case Success(state) => complete(write(state))
            case Failure(ex) => throw ex
          }
        }
      }


      //TODO add rest serializer to Creature
      /*~
      put {
        entity(as[Creature]) { creature =>
          creatureManager ! AddCreature(creature)
          complete(StatusCodes.OK)
        }
      }*/
    }

  private val wsRoutes: Route =
    pathEnd {
      get {
        handleWebSocketMessages(websocketChatFlow)
      }
    }

  private def websocketChatFlow: Flow[Message, Message, Any] =
    Flow[Message]
      .collect {
        case TextMessage.Strict(msg) ⇒ msg // unpack incoming WS text messages...
      }
      .via(dispatcherFlow) // ... and route them through the chatFlow ...
      .map {
      case msg: String =>
        TextMessage.Strict(msg)
    }
      .via(reportErrorsFlow) // ... then log any processing errors on stdin

  private def dispatcherFlow: Flow[String, String, Any] = {

    // A source that will create a target ActorRef per
    // materialization where the chatActor will send its messages to.
    // This source will only buffer one element and will fail if the client doesn't read
    // messages fast enough.
    val source = Source.actorRef[String](Config.batchSize, OverflowStrategy.fail)
      .mapMaterializedValue(sinkActor ! ClientJoined(_))

    Flow.fromSinkAndSource(Sink.ignore, source)
  }

  private def reportErrorsFlow[T]: Flow[T, T, Any] =
    Flow[T]
      .watchTermination()((_, f) => f.onComplete {
        case Failure(cause) =>
          println(s"WS stream failed with $cause")
        case _ => // ignore regular completion
      })
}


object Webservice {
  val defaultHeaders = List(RawHeader("Access-Control-Allow-Origin", "*"), RawHeader("Access-Control-Allow-Methods","POST, DELETE, OPTIONS"))
}