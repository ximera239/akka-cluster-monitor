package com.example.backend

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorLogging
import akka.actor.Actor
import com.example.logging.Logging

@deprecated
class SimpleClusterListener extends Actor with Logging {

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      log.info(s"Member is Up: ${member.address}")
    case UnreachableMember(member) =>
      log.info(s"Member detected as unreachable: $member")
    case MemberRemoved(member, previousStatus) =>
      log.info(s"Member is Removed: ${member.address} after $previousStatus")
    case _: MemberEvent => // ignore
  }
}