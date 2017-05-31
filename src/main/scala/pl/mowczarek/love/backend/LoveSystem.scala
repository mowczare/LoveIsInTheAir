package pl.mowczarek.love.backend

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import pl.mowczarek.love.backend.actors.CreatureManager.StartGame
import pl.mowczarek.love.backend.actors.{CreatureManager, Field, SystemMap}
import pl.mowczarek.love.backend.config.Config
import pl.mowczarek.love.backend.socket.{DispatcherActor, Webservice}

import scala.util.{Failure, Success}

/**
  * Created by neo on 15.03.17.
  */
object LoveSystem extends App {
  implicit val actorSystem = ActorSystem(Config.systemName)

  import actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()

  val sinkActor: ActorRef = actorSystem.actorOf(DispatcherActor.clusterSingletonProps, "sinkActor")

  val systemMap = actorSystem.actorOf(SystemMap.clusterSingletonProps(sinkActor), "systemMap")

  val fieldsRegion: ActorRef = Field.clusterShardingProps(systemMap, sinkActor)

  val creatureManager = actorSystem.actorOf(CreatureManager.props(systemMap)) // TODO Merge with systemMap
  creatureManager ! StartGame   // TODO send with init in rest

  val service = new Webservice(sinkActor, creatureManager, systemMap) // TODO Move with binding to sinkActor

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
