package com.example.backend

import akka.actor.{Actor, Props}
import com.example.logging.Logging

/**
  * Created by ezhoga on 05.11.16.
  */
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