package com.example.backend

import akka.actor.{Actor, Cancellable, Props}
import akka.persistence.PersistentActor
import com.example.logging.Logging

import scala.concurrent.duration._

/**
  * Created by ezhoga on 05.11.16.
  */
class ShardWorker extends Actor with Logging with PersistentActor {
  var checker: Checker = _
  var cancellable: Cancellable = _

  override def persistenceId: String = "Checker-" + self.path.name

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

    }

  override def receiveRecover: Receive = {
    case msg: ClusterMessage ⇒ processMessage(msg)
  }


  def receiveCommand = {
    case msg: ClusterMessage =>
      persist(msg)(processMessage)
  }
}

object ShardWorker {
  def props = Props(new ShardWorker)
  def actorName(s: String) = s
}