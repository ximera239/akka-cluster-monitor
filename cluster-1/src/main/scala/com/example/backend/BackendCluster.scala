package com.example.backend

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.{ClusterSharding, ClusterSingletonManager, ClusterSingletonProxy, ShardRegion}
import akka.persistence.journal.leveldb.SharedLeveldbStore
import com.example.Environment
import com.example.logging.Logging
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
/**
  * Created by ezhoga on 26.08.16.
  */
object BackendCluster extends App {
  val Name = "some-name"
  val System = ActorSystem("ClusterSystem", Environment.config)

  val store = System.actorOf(Props[SharedLeveldbStore], "store")

  val worker = {
    val idExtractor: ShardRegion.IdExtractor = {
      case m: ClusterMessage ⇒ (m.id, m)
    }
    val shardResolver: ShardRegion.ShardResolver = {
      case m: ClusterMessage ⇒ (m.id.hashCode % 12).toString
    }

    val worker: ActorRef = ClusterSharding(System).start(
      typeName = "check-api",
      entryProps = Some(ShardWorker.props),
      idExtractor = idExtractor,
      shardResolver = shardResolver)

    worker
  }

  System.scheduler.scheduleOnce(1.seconds, worker, CheckHTTP("http://www.facebook.com"))(System.dispatcher)
  System.scheduler.scheduleOnce(2.seconds, worker, CheckHTTP("http://www.twitter.com"))(System.dispatcher)

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









