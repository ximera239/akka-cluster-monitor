package com.example.cluster1

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.{ClusterSharding, ClusterSingletonManager, ClusterSingletonProxy, ShardRegion}
import com.example.logging.Logging

/**
  * Created by ezhoga on 26.08.16.
  */
object Cluster1Server extends App {
  val Name = "some-name"
  val system = ActorSystem("ClusterSystem")
  system.actorOf(Props(new SimpleClusterListener), "simple-cluster-listener")




  val masterActor = {
    system.actorOf(ClusterSingletonManager.props(
      singletonProps = Props(classOf[SimpleClusterListener]),
      singletonName = s"$Name-worker",
      terminationMessage = PoisonPill,
      role = Some("backend")),
      name = s"$Name-singleton"
    )

    val a = system.actorOf(ClusterSingletonProxy.props(
      singletonPath = s"/user/$Name-singleton/$Name-worker",
      role = Some("backend")
    ), Name)

    class ShardWorker extends Actor with Logging {
      def actorFactory(ourCheckType: String, version: Long):Props = {
        Props.empty // stub
      }

      def receive = {
        case GetClusteredActor(messageId) =>
          log.trace(s"Clustered store-api actor proxy get message: GetClusteredActor(some message id)")
          val _sender = sender()
          val storeActor = context.child("stub"/*actorName(messageId)*/).fold {
            Some(Actor.noSender)
          } {ref => Some(ref)}.fold {
            _sender ! ClusteredActorIsUnavailable
          } {ref =>
            _sender ! ClusteredActorResponse(ref)
          }
        case StopClusteredActor(storeId) =>
          log.trace(s"Clustered store-api actor proxy get message: StopClusteredActor($storeId)")
          context.child("stub"/*actorName(storeId)*/).foreach {ref =>
            context.stop(ref)
          }
      }
    }

    object ShardWorker {
      def props = Props(new ShardWorker)
    }

    val idExtractor: ShardRegion.IdExtractor = {
      case m@GetClusteredActor(id) ⇒ (id.toString, m)
      case m@StopClusteredActor(id) ⇒ (id.toString, m)
    }
    val shardResolver: ShardRegion.ShardResolver = {
      case m@GetClusteredActor(id) ⇒ (id % 12).toString
      case m@StopClusteredActor(id) ⇒ (id % 12).toString
    }
    val worker: ActorRef = ClusterSharding(system).start(
      typeName = "check-api",
      entryProps = Some(ShardWorker.props),
      idExtractor = idExtractor,
      shardResolver = shardResolver)

    a
  }


  sys.addShutdownHook {
    system.shutdown()
  }
}


case class GetClusteredActor(messageId: Long)
case class StopClusteredActor(messageId: Long)
case object ClusteredActorIsUnavailable
case class ClusteredActorResponse(ref: ActorRef)