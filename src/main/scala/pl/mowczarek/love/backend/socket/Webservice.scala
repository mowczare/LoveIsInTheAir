package pl.mowczarek.love.backend.socket

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import pl.mowczarek.love.backend.actors.ActorEvent
import pl.mowczarek.love.backend.actors.CreatureManager.{AddCreature, AddRandomCreature, KillAllCreatures}
import pl.mowczarek.love.backend.config.Config
import pl.mowczarek.love.backend.socket.DispatcherActor.ClientJoined

import scala.util.Failure

/**
  * Created by neo on 03.04.17.
  */
class Webservice(sinkActor: ActorRef, creatureManager: ActorRef)(implicit fm: Materializer, system: ActorSystem) extends Directives {
  import system.dispatcher

  def route: Route =
    path("creature") {
      restRoutes
    } ~
    path("ws") {
      wsRoutes
    }

  private val restRoutes: Route =
    pathEnd {
      delete {
        creatureManager ! KillAllCreatures
        complete(StatusCodes.NoContent)
      } ~
      post {
        creatureManager ! AddRandomCreature
        complete(StatusCodes.OK)
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
      case msg: ActorEvent ⇒
        TextMessage.Strict(msg.toString) // ... TODO pack outgoing messages into WS JSON messages ...
    }
      .via(reportErrorsFlow) // ... then log any processing errors on stdin

  private def dispatcherFlow: Flow[String, ActorEvent, Any] = {

    // A source that will create a target ActorRef per
    // materialization where the chatActor will send its messages to.
    // This source will only buffer one element and will fail if the client doesn't read
    // messages fast enough.
    val source = Source.actorRef[ActorEvent](Config.batchSize, OverflowStrategy.fail)
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