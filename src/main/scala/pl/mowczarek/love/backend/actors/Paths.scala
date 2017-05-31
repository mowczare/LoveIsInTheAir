package pl.mowczarek.love.backend.actors

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem}
import akka.cluster.sharding.ClusterSharding
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonProxy, ClusterSingletonProxySettings}

/**
  * Created by neo on 31.05.17.
  */
trait PathsActor extends Actor {

  this: Actor =>

  import Paths._

  val systemMap = context.system.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = s"/user/$mapName",
      settings = ClusterSingletonProxySettings(context.system)),
    name = "mapProxy")

  val sinkActor = context.system.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = s"/user/$sinkName",
      settings = ClusterSingletonProxySettings(context.system)),
    name = "sinkActorProxy")

  val creatureManager = context.system.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = s"/user/$creatureManagerName",
      settings = ClusterSingletonProxySettings(context.system)),
    name = "creatureManagerProxy")

  def fieldsPath = Paths.fieldsPath(context.system)

}

object Paths {

  private var systemMapState: Option[ActorRef] = None

  private var sinkActorState: Option[ActorRef] = None

  private var creatureManagerState: Option[ActorRef] = None

  def fieldsPath(implicit system: ActorSystem) = ClusterSharding(system).shardRegion(Paths.fieldsName)

  def systemMap(implicit system: ActorSystem) = systemMapState.getOrElse {
    val proxy = system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/$mapName",
        settings = ClusterSingletonProxySettings(system)),
      name = "mapProxy")
    systemMapState = Some(proxy)
    proxy
  }

  def sinkActor(implicit system: ActorSystem) = sinkActorState.getOrElse {
    val proxy = system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/$sinkName",
        settings = ClusterSingletonProxySettings(system)),
      name = "sinkActorProxy")
    sinkActorState = Some(proxy)
    proxy
  }

  def creatureManager(implicit system: ActorSystem) = creatureManagerState.getOrElse {
    val proxy = system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/$creatureManagerName",
        settings = ClusterSingletonProxySettings(system)),
      name = "creatureManagerProxy")
    creatureManagerState = Some(proxy)
    proxy
  }

  // TODO move to config
  val mapName = "systemMap"
  val sinkName = "sinkActor"
  val creatureManagerName = "creatureManager"
  val fieldsName = "Field"
}
