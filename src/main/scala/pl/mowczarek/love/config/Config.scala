package pl.mowczarek.love.config

import com.typesafe.config.ConfigFactory

/**
  * Created by neo on 22.03.17.
  */
object Config {
  private val conf = ConfigFactory.load()

  val mapSize = conf.getInt("loveIsInTheAir.system.mapSize")
  val creaturesAtStart = conf.getInt("loveIsInTheAir.system.creaturesAtStart")
}
