package pl.mowczarek.love.backend.config

import com.typesafe.config.ConfigFactory

/**
  * Created by neo on 22.03.17.
  */
object Config {
  private val conf = ConfigFactory.load()

  lazy val mapSize = conf.getInt("loveIsInTheAir.system.mapSize")
  lazy val creaturesAtStart = conf.getInt("loveIsInTheAir.system.creaturesAtStart")

  lazy val batchSize = conf.getInt("loveIsInTheAir.system.websocket.batchSize")

  lazy val host = conf.getString("loveIsInTheAir.system.websocket.host")
  lazy val port = conf.getInt("loveIsInTheAir.system.websocket.port")

  lazy val numberOfNodes = conf.getInt("loveIsInTheAir.system.numberOfNodes")

  lazy val numberOfShards = 10 * numberOfNodes

}
