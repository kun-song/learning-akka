package com.akkademy

import akka.actor.{Actor, Status}
import akka.event.Logging
import com.akkademy.messages.{GetRequest, KeyNotFoundException, SetRequest}

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
    case SetRequest(key, value) ⇒
      log.info(s"received SetRequest = key: $key, value: $value")
      map.put(key, value)
      sender() ! Status.Success       // 成功响应
    case GetRequest(key)        ⇒
      log.info(s"received GetRequest = key: $key")
      map.get(key) match {
        case Some(v)  ⇒ sender() ! v  // 响应 value 值
        case None     ⇒ sender() ! Status.Failure(KeyNotFoundException(key))  // 失败响应
      }
    case unknown                ⇒
      log.info(s"received unknown message = $unknown")
      sender() ! Status.Failure(new ClassNotFoundException)  // 失败响应
  }
}
