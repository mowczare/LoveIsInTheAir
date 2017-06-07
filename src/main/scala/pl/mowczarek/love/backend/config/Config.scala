package pl.mowczarek.love.backend.config

import com.typesafe.config.ConfigFactory

/**
  * Created by neo on 22.03.17.
  */
object Config {
  private val conf = ConfigFactory.load()

  lazy val systemName = conf.getString("loveIsInTheAir.system.name")

  lazy val mapSize = conf.getInt("loveIsInTheAir.system.mapSize")
  lazy val creaturesAtStart = conf.getInt("loveIsInTheAir.system.creaturesAtStart")

  lazy val batchSize = conf.getInt("loveIsInTheAir.system.websocket.batchSize")

  lazy val host = conf.getString("loveIsInTheAir.system.websocket.host")
  lazy val port = conf.getInt("loveIsInTheAir.system.websocket.port")

  lazy val numberOfNodes = conf.getInt("loveIsInTheAir.system.numberOfNodes")

  lazy val numberOfShards = 10 * numberOfNodes


  // Creatures algo

  lazy val chanceOfReproducingWhileCopulating = conf.getInt("loveIsInTheAir.system.chanceOfReproducingWhileCopulating")
  lazy val initialFuss = conf.getDouble("loveIsInTheAir.system.initialFuss")
  lazy val initialDesperation = conf.getDouble("loveIsInTheAir.system.initialDesperation")
  lazy val desperationIncreaseFactor = conf.getDouble("loveIsInTheAir.system.desperationIncreaseFactor")
  lazy val matureTime = conf.getInt("loveIsInTheAir.system.matureTime")
  lazy val lifeTime = conf.getInt("loveIsInTheAir.system.lifeTime")
  lazy val lifeTimeDelta = conf.getInt("loveIsInTheAir.system.lifeTimeDelta")
  lazy val firstMigrationTime = conf.getInt("loveIsInTheAir.system.firstMigrationTime")
  lazy val firstMigrationTimeDelta = conf.getInt("loveIsInTheAir.system.firstMigrationTimeDelta")
  lazy val nextMigrationsTime = conf.getInt("loveIsInTheAir.system.nextMigrationsTime")
  lazy val nextMigrationsTimeDelta = conf.getInt("loveIsInTheAir.system.nextMigrationsTimeDelta")
  lazy val accostingFrequency = conf.getInt("loveIsInTheAir.system.accostingFrequency")
  lazy val copulationFrequency = conf.getInt("loveIsInTheAir.system.copulationFrequency")
  lazy val pregnancyTime = conf.getInt("loveIsInTheAir.system.pregnancyTime")

}
