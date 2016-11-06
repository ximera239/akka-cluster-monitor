package com.example.cluster2

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.{ClusterSingletonManager, ClusterSingletonProxy}

/**
  * Created by ezhoga on 26.08.16.
  */
object Cluster2Server extends App {

  val System = ActorSystem("ClusterSystem")

  val Name = "some-name"
  val sv = {
    System.actorOf(ClusterSingletonManager.props(
      singletonProps = SimpleClusterListener.props,
      singletonName = s"$Name-worker",
      terminationMessage = PoisonPill,
      role = Some("backend")),
      name = s"$Name-singleton"
    )

    System.actorOf(ClusterSingletonProxy.props(
      singletonPath = s"/user/$Name-singleton/$Name-worker",
      role = Some("backend")
    ), Name)
  }

//  sys.addShutdownHook {
//    System.shutdown()
//  }
}
