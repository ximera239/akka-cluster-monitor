package com.example.backend

import akka.actor.{Actor, Props}
import com.example.logging.Logging

import scala.concurrent.duration._
/**
  * Created by ezhoga on 05.11.16.
  */
class HttpChecker(http: String) extends Checker with Logging {
  def process() = {
    log.info(s"I'm checking $http")
  }
}
