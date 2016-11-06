package com.example.backend

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSystem, Identify, PoisonPill, Props}
import akka.cluster.Cluster
import akka.contrib.pattern.{ClusterSharding, ClusterSingletonManager, ClusterSingletonProxy, ShardRegion}
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import com.example.Environment
import com.example.backend.http.{SimpleSuperviser, SprayRoutes}
import com.example.logging.Logging
import com.typesafe.config.ConfigFactory
import akka.io.IO
import spray.can.Http

import scala.concurrent.duration._
/**
  * Created by ezhoga on 26.08.16.
  */
object BackendCluster extends App with Logging {
  val Name = "some-name"
  val System = ActorSystem("ClusterSystem", Environment.config)



  val worker = {
    val idExtractor: ShardRegion.IdExtractor = {
      case m@CheckHTTP(url) ⇒
        log.info(s"Called idExtractor -> for CheckHTTP($url)")
        (url, m)
      case m: ClusterMessage ⇒ (m.id, m)
      case m@Identify(s: String) => (s, m)
    }
    val shardResolver: ShardRegion.ShardResolver = {
      case m@CheckHTTP(url) ⇒
        log.info(s"Called shardResolver -> for CheckHTTP($url)")
        (Math.abs(url.hashCode) % 12).toString
      case m: ClusterMessage ⇒ (Math.abs(m.id.hashCode) % 12).toString
      case Identify(s: String) => (Math.abs(s.hashCode) % 12).toString
    }

    val worker: ActorRef = ClusterSharding(System).start(
      typeName = "check-api",
      entryProps = Some(ShardWorker.props),
      idExtractor = idExtractor,
      shardResolver = shardResolver)

    worker
  }

  val sv = {
    System.actorOf(ClusterSingletonManager.props(
      singletonProps = SimpleSuperviser.props(worker),
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


  val service = System.actorOf(SprayRoutes.props(sv), "simple-service")

  IO(Http)(System) ! Http.Bind(service, "localhost", port = (Environment.config.getInt("akka.remote.netty.tcp.port") + 8888 - 2551))

/*
  System.scheduler.scheduleOnce(5.seconds) {
    log.info(s"Sending facebook message")
    worker ! CheckHTTP("http://www.facebook.com")
  }(System.dispatcher)

  System.scheduler.scheduleOnce(6.seconds) {
    log.info(s"Sending twitter message")
    worker ! CheckHTTP("http://www.twitter.com")
  }(System.dispatcher)
*/

  sys.addShutdownHook {
    System.shutdown()
  }

}

sealed trait ClusterMessage {
  def id: String
  def isStop: Boolean
}

case class CheckHTTP(url: String) extends ClusterMessage {
  val id = url
  val isStop = false
}
case class StopCheckHTTP(url: String) extends ClusterMessage {
  val id = url
  val isStop = true
}

case object ClusteredActorIsUnavailable
case class ClusteredActorResponse(ref: ActorRef)









