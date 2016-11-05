package com.example

import com.example.logging.Logging
import com.typesafe.config.ConfigFactory

/**
  * Created by ezhoga on 05.11.16.
  */
object Environment extends Logging {
  private val configLocations = List(
    "application.conf"
  )

  private val envAkkaEnv =
    ConfigFactory.systemEnvironment().withOnlyPath("akka")

  val config = {
    configLocations.reverse.foldLeft(envAkkaEnv) {
      case (compoundConfig, next) if next.startsWith("/") =>
        compoundConfig.withFallback(ConfigFactory.parseURL(getClass.getResource(next)))
      case (compoundConfig, next) =>
        compoundConfig.withFallback(ConfigFactory.load(next))
    }
  }
}
