package pl.mowczarek.love.backend.socket

/**
  * Created by neo on 03.04.17.
  */

import akka.actor._
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import pl.mowczarek.love.backend.config.Config
import pl.mowczarek.love.backend.socket.DispatcherActor.ClientJoined

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


class DispatcherActor extends Actor with ActorLogging {

  private var subscribers = Set.empty[ActorRef]

  override def preStart = {
    implicit val materializer = ActorMaterializer()
    val service = new Webservice(materializer, context.system)

    val bindingFuture = Http(context.system).bindAndHandle(service.route, Config.host, Config.port)
    bindingFuture.onComplete {
      case Success(binding) =>
        val localAddress = binding.localAddress
        println(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
      case Failure(e) =>
        println(s"Binding failed with ${e.getMessage}")
        context.system.terminate()
    }
  }

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
      settings = ClusterSingletonManagerSettings(system))

  sealed trait FrontendEvent
  case class ClientJoined(subscriber: ActorRef) extends FrontendEvent
}