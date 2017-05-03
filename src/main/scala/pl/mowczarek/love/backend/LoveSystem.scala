package pl.mowczarek.love.backend

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import pl.mowczarek.love.backend.actors.CreatureManager.StartGame
import pl.mowczarek.love.backend.actors.{CreatureManager, SystemMap}
import pl.mowczarek.love.backend.config.Config
import pl.mowczarek.love.backend.socket.{DispatcherActor, Webservice}

import scala.util.{Failure, Success}

/**
  * Created by neo on 15.03.17.
  */
object LoveSystem extends App {
  implicit val actorSystem = ActorSystem("loveIsInTheAir")

  import actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()

  val sinkActor: ActorRef = actorSystem.actorOf(Props(new DispatcherActor))

  val systemMap = actorSystem.actorOf(SystemMap.props(sinkActor))
  Thread.sleep(2000) // wait for map to create TODO dont use thread sleep ffs
  val creatureManager = actorSystem.actorOf(CreatureManager.props(systemMap))
  creatureManager ! StartGame

  val service = new Webservice(sinkActor, creatureManager)

  val bindingFuture = Http().bindAndHandle(service.route, Config.host, Config.port)
  bindingFuture.onComplete {
    case Success(binding) =>
      val localAddress = binding.localAddress
      println(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
    case Failure(e) =>
      println(s"Binding failed with ${e.getMessage}")
      actorSystem.terminate()
  }


}
