package pl.mowczarek.love

import akka.actor.ActorSystem
import pl.mowczarek.love.actors.CreatureActor

/**
  * Created by neo on 15.03.17.
  */
object System extends App {
  implicit val actorSystem = ActorSystem("loveIsInTheAir")

  val actors = (1 to 1000).map { _ =>
    actorSystem.actorOf(CreatureActor.props)
  }

}
