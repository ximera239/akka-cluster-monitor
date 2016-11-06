package com.example.backend.http

import java.io.{File, FileWriter}

import akka.actor.{Actor, ActorIdentity, ActorRef, Props}
import akka.cluster.ClusterEvent.{MemberEvent, MemberExited, MemberRemoved, UnreachableMember}
import akka.persistence.PersistentActor
import com.example.backend.{CheckHTTP, ClusterMessage}
import com.example.logging.Logging

import scala.io.Source

/**
  * Created by ezhoga on 05.11.16.
  */
class SimpleSuperviser(shardWorker: ActorRef) extends Actor with Logging {


  override def preStart() {
//    val cluster = Cluster(context.system)
//    cluster.subscribe(self, classOf[MemberEvent])
//    cluster.subscribe(self, classOf[UnreachableMember])

    new File("/tmp/journal/").mkdirs()
    if (new File("/tmp/journal/ttt").exists()) {
      Source.fromFile("/tmp/journal/ttt").getLines().foreach {
        case url if url.nonEmpty =>
          setup(CheckHTTP(url))
      }
    }
  }


  def setup(msg: ClusterMessage) = {
    shardWorker ! msg
  }

  def persist(msg: ClusterMessage)(f: ClusterMessage => Unit) = {
    new File("/tmp/journal/").mkdirs()
    msg match {
      case CheckHTTP(url) =>
        val fw = new FileWriter("/tmp/journal/ttt", true)
        fw.write(s"$url\n")
        fw.close()
        f(msg)
    }
  }

  def receive = {
    case msg: ClusterMessage =>
      persist(msg)(setup)

    case ActorIdentity(s: String, ref) =>

    case UnreachableMember(member) =>

//      recoverAddress(member.address)

    case MemberRemoved(member, _) =>
//      removeAddress(member.address)

    case MemberExited(member) =>
//      removeAddress(member.address)

  }

}

object SimpleSuperviser {
  def props(shardWorker: ActorRef) = Props(new SimpleSuperviser(shardWorker))
}