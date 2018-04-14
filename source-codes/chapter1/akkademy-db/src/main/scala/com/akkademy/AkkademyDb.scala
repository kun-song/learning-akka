package com.akkademy

import akka.actor.Actor
import akka.event.Logging
import com.akkademy.messages.SetRequest

import scala.collection.mutable

/**
  * Author: Kyle Song
  * Date:   PM9:05 at 18/4/14
  * Email:  satansk@hotmail.com
  */
class AkkademyDb extends Actor {

  val map = new mutable.HashMap[String, Object]
  val log = Logging(context.system, this)

  override def receive: Receive = {
    /**
      * 1. 若接收到 SetRequest 消息
      */
    case SetRequest(key, value) ⇒
      log.info(s"received SetRequest = key: $key, value: $value")
      map.put(key, value)
    /**
      * 2. 未知类型的消息
      */
    case unknown                ⇒ log.info(s"received unknown message = $unknown")
  }
}
