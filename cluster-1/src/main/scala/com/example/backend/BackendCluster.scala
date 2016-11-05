package com.example.backend

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.{ClusterSharding, ClusterSingletonManager, ClusterSingletonProxy, ShardRegion}
import com.example.logging.Logging
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
/**
  * Created by ezhoga on 26.08.16.
  */
object BackendCluster extends App {
  val Name = "some-name"
  val System = ActorSystem("ClusterSystem")

  val worker = {
    val idExtractor: ShardRegion.IdExtractor = {
      case m@CheckHTTP(id) ⇒ (id, m)
      case m@StopCheckHTTP(id) ⇒ (id, m)
    }
    val shardResolver: ShardRegion.ShardResolver = {
      case m@CheckHTTP(id) ⇒ (id.hashCode % 12).toString
      case m@StopCheckHTTP(id) ⇒ (id.hashCode % 12).toString
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

case class CheckHTTP(url: String)
case class StopCheckHTTP(url: String)
case object ClusteredActorIsUnavailable
case class ClusteredActorResponse(ref: ActorRef)









