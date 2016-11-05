package com.example

import com.typesafe.config.ConfigFactory

/**
  * Created by ezhoga on 05.11.16.
  */
class Environment {
  private val configLocations = List(
    "application.conf"
  )

  val config = configLocations.reverse.foldLeft(ConfigFactory.empty()) {
    case (compoundConfig, next) if next.startsWith("/") =>
      compoundConfig.withFallback(ConfigFactory.parseURL(getClass.getResource(next)))
    case (compoundConfig, next) =>
      compoundConfig.withFallback(ConfigFactory.load(next))
  }
}
