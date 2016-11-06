package com.example.backend.http

import akka.actor.{Actor, ActorRef, Props}
import com.example.backend.{BackendCluster, CheckHTTP}
import spray.routing.HttpService

/**
  * Created by ezhoga on 05.11.16.
  */
class SprayRoutes(worker: ActorRef) extends Actor with HttpService{
  def actorRefFactory = context
  def receive = runRoute(route)

  val route = {
    get {
      path(Segment) {
        p =>
          worker ! CheckHTTP(p)
          complete("Ok")
      }
    }
  }
}

object SprayRoutes {
  def props(worker: ActorRef) = Props(new SprayRoutes(worker))
}
