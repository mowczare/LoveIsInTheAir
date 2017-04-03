package pl.mowczarek.love.backend.config

import com.typesafe.config.ConfigFactory

/**
  * Created by neo on 22.03.17.
  */
object Config {
  private val conf = ConfigFactory.load()

  val mapSize = conf.getInt("loveIsInTheAir.system.mapSize")
  val creaturesAtStart = conf.getInt("loveIsInTheAir.system.creaturesAtStart")

  val batchSize = conf.getInt("loveIsInTheAir.system.websocket.batchSize")

  val host = conf.getString("loveIsInTheAir.system.websocket.host")
  val port = conf.getInt("loveIsInTheAir.system.websocket.port")
}
