package pl.mowczarek.love.backend.socket

/**
  * Created by neo on 03.04.17.
  */

import akka.actor._
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import pl.mowczarek.love.backend.actors.ActorEvent
import pl.mowczarek.love.backend.socket.DispatcherActor.ClientJoined


class DispatcherActor extends Actor with ActorLogging {

  private var subscribers = Set.empty[ActorRef]

  override def receive: Receive = {
    case ClientJoined(subscriber) ⇒
      context.watch(subscriber)
      subscribers += subscriber
    case ev: String =>
      dispatch(ev)
    case Terminated(sub) ⇒
      subscribers = subscribers.filterNot(_ == sub)
  }

  private def dispatch(event: Any): Unit = subscribers.foreach(_ forward event)

}

object DispatcherActor {

  def props = Props(new DispatcherActor)

  def clusterSingletonProps(implicit system: ActorSystem): Props =
    ClusterSingletonManager.props(
      singletonProps = props,
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system).withRole("worker"))

  sealed trait FrontendEvent
  case class ClientJoined(subscriber: ActorRef) extends FrontendEvent
}