package com.satansk

import akka.actor.{Actor, Stash}
import akka.util.Timeout
import com.satansk.ClientActorByBecome.{Connected, Disconnected, Get}
import com.satansk.ClientActorTimeout.CheckConnected

import scala.concurrent.TimeoutException

/**
  * Author: Song Kun
  * Date:   上午7:53 at 18/4/19
  * Email:  satansk@hotmail.com
  */
class ClientActorTimeout(timeout: Timeout) extends Actor with Stash {
  import context._

  override def preStart(): Unit = {
    system.scheduler.scheduleOnce(timeout.duration, self, CheckConnected)
  }

  /**
    * 离线状态（默认）
    */
  override def receive: Receive = {
    case Get(key)       ⇒ stash()
    case Connected      ⇒ become(online); unstashAll()

    /**
      * 仅离线状态需要处理 CheckConnected 消息
      */
    case CheckConnected ⇒ throw new TimeoutException
  }

  /**
    * 在线状态
    */
  def online: Receive = {
    case Get(key)     ⇒ ClientActorTimeout processMessage key
    case Disconnected ⇒ unbecome()
  }
}

object ClientActorTimeout {
  case class Get(key: String)
  case object Connected
  case object Disconnected
  case object CheckConnected

  def processMessage(m: String): String = m.reverse
}
