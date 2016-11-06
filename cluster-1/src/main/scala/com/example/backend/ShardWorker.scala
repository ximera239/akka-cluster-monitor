package com.example.backend

import akka.actor.{Actor, ActorIdentity, ActorRef, Cancellable, Identify, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberEvent, MemberExited, MemberRemoved, UnreachableMember}
import akka.persistence.PersistentActor
import akka.persistence.journal.leveldb.SharedLeveldbJournal
import com.example.logging.Logging

import scala.concurrent.duration._

/**
  * Created by ezhoga on 05.11.16.
  */
class ShardWorker extends Actor with Logging {
  var checker: Checker = _
  var cancellable: Cancellable = _

  override def preStart(): Unit = {
    log.info(s"New ShardWorker created -> ${self.path.toSerializationFormat}")

        val cluster = Cluster(context.system)
        cluster.subscribe(self, classOf[MemberEvent])
        cluster.subscribe(self, classOf[UnreachableMember])
    super.preStart()
  }

  override def postStop(): Unit = {
    val cluster = Cluster(context.system)
    cluster.unsubscribe(self)

    super.postStop()
  }

  def processMessage(msg: ClusterMessage) =
    msg match {
      case CheckHTTP(url) if Option(cancellable).isEmpty =>
        log.trace(s"Clustered actor proxy get message: CheckHTTP($url)")
        val _sender = sender()

        import context.dispatcher
        checker = Option(checker).getOrElse(new HttpChecker(url))

        cancellable = context.system.scheduler.schedule(10.seconds, 1.second) {
          checker.process()
        }

      case CheckHTTP(url) =>
        log.trace(s"CheckHTTP($url) is already scheduled")

      case StopCheckHTTP(url) if Option(cancellable).isDefined =>
        log.trace(s"CheckHTTP($url) will be stopped")
        cancellable.cancel()
        cancellable = null

      case StopCheckHTTP(url) =>
        log.trace(s"CheckHTTP($url) is not running")

      case something =>
        log.trace(s"What's this? $something")
    }

  def receive = {
    case msg: ClusterMessage =>
      processMessage(msg)


    case UnreachableMember(member) =>

      log.info(s"Member ${member.address} -> UnreachableMember")

    //      recoverAddress(member.address)

    case MemberRemoved(member, _) =>
    //      removeAddress(member.address)

      log.info(s"Member ${member.address} -> MemberRemoved")

    case MemberExited(member) =>
    //      removeAddress(member.address)
      log.info(s"Member ${member.address} -> MemberExited")
  }
}

object ShardWorker {
  def props = Props(new ShardWorker)
  def actorName(s: String) = s
}