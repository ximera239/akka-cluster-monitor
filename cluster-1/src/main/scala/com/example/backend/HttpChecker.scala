package com.example.backend

import akka.actor.{Actor, Props}
import com.example.logging.Logging

import scala.concurrent.duration._
/**
  * Created by ezhoga on 05.11.16.
  */
class HttpChecker(http: String) extends Checker with Actor with Logging {
  override def preStart(): Unit = {
    import context.dispatcher

    context.system.scheduler.schedule(1.second, 1.seconds) {
      log.info(s"I'm checking $http")
    }
  }
  def receive = Actor.emptyBehavior
}

object HttpChecker {
  def props(url: String) = Props(new HttpChecker(url: String))
}