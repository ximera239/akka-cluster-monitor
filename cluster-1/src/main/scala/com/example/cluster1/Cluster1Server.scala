package com.example.cluster1

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.{ClusterSharding, ClusterSingletonManager, ClusterSingletonProxy, ShardRegion}
import com.example.logging.Logging
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
/**
  * Created by ezhoga on 26.08.16.
  */
object Cluster1Server extends App {
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

class ShardWorker extends Actor with Logging {
  def actorFactory(checkClass: String, version: Long):Props = {
    Props(Class.forName(checkClass), version)
  }

  def receive = {
    case msg@CheckHTTP(url) =>
      log.trace(s"Clustered actor proxy get message: CheckHTTP($url)")
      val _sender = sender()

      val actor = context.child(ShardWorker.actorName(url)).fold {
        Some(context.actorOf(HttpChecker.props(url)))
      } {ref => Some(ref)}.fold {
        _sender ! ClusteredActorIsUnavailable
      } {ref =>
        ref.tell(msg, _sender)
      }

    case StopCheckHTTP(url) =>
      log.trace(s"Clustered store-api actor proxy get message: StopCheckHTTP($url)")
      context.child(ShardWorker.actorName(url)).foreach {ref =>
        context.stop(ref)
      }
  }
}

object ShardWorker {
  def props = Props(new ShardWorker)
  def actorName(s: String) = s
}

case class CheckHTTP(url: String)
case class StopCheckHTTP(url: String)
case object ClusteredActorIsUnavailable
case class ClusteredActorResponse(ref: ActorRef)


trait Checker

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

class SomeChecker extends Checker
class AnotherChecker extends Checker


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