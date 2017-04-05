package pl.mowczarek.love.backend.socket

/**
  * Created by neo on 03.04.17.
  */

import akka.actor._
import pl.mowczarek.love.backend.actors.ActorEvent
import pl.mowczarek.love.backend.socket.DispatcherActor.ClientJoined


class DispatcherActor extends Actor with ActorLogging {

  private var subscribers = Set.empty[ActorRef]

  override def receive: Receive = {
    case ev: ActorEvent =>
      dispatch(ev)
    case ClientJoined(subscriber) ⇒
      context.watch(subscriber)
      subscribers += subscriber
    //TODO send full SystemMap with creatures
    case Terminated(sub) ⇒
      subscribers = subscribers.filterNot(_ == sub)
  }

  private def dispatch(event: ActorEvent): Unit = subscribers.foreach(_ forward event)

}

object DispatcherActor {
  sealed trait FrontendEvent
  case class ClientJoined(subscriber: ActorRef) extends FrontendEvent
}