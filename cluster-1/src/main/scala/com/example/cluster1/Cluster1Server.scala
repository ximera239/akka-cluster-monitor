package com.example.cluster1

import akka.actor.{ActorSystem, Props}

/**
  * Created by ezhoga on 26.08.16.
  */
object Cluster1Server extends App {

  val system = ActorSystem("ClusterSystem")
  system.actorOf(Props(new SimpleClusterListener), "simple-cluster-listener")

  sys.addShutdownHook {
    system.shutdown()
  }
}
