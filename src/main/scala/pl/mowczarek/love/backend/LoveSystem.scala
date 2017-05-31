package pl.mowczarek.love.backend

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.singleton.ClusterSingletonManager
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

  val sinkActor: ActorRef = actorSystem.actorOf(DispatcherActor.clusterSingletonProps, "sinkActor")

  val systemMap: ActorRef = actorSystem.actorOf(SystemMap.clusterSingletonProps, "systemMap")

  val fieldsRegion: ActorRef = Field.clusterShardingProps

  val creatureManager = actorSystem.actorOf(CreatureManager.clusterSingletonProps, "creatureManager") // TODO Merge with systemMap




  //creatureManager ! StartGame   // TODO send with init in rest


}
