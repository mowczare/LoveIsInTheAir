package pl.mowczarek.love

import akka.actor.ActorSystem
import pl.mowczarek.love.actors.CreatureGenerator.StartGame
import pl.mowczarek.love.actors.{CreatureGenerator, SystemMap}

import scala.concurrent.Await

/**
  * Created by neo on 15.03.17.
  */
object System extends App {
  implicit val actorSystem = ActorSystem("loveIsInTheAir")

  val systemMap = actorSystem.actorOf(SystemMap.props)
  Thread.sleep(2000) // wait for map to create TODO dont use thread sleep ffs
  val creatureGenerator = actorSystem.actorOf(CreatureGenerator.props(systemMap))
  creatureGenerator ! StartGame
}
