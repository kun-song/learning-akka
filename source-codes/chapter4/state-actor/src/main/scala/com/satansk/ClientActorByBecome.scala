package com.satansk

import akka.actor.{Actor, Stash}
import com.satansk.ClientActorByBecome.{Connected, Disconnected, Get}

/**
  * Author: Song Kun
  * Date:   上午7:53 at 18/4/19
  * Email:  satansk@hotmail.com
  */
class ClientActorByBecome extends Actor with Stash {
  import context._

  /**
    * 离线状态（默认）
    */
  override def receive: Receive = {
    case Get(key)   ⇒ stash()
    case Connected  ⇒ become(online); unstashAll()
  }

  /**
    * 在线状态
    */
  def online: Receive = {
    case Get(key)     ⇒ ClientActorByBecome processMessage key
    case Disconnected ⇒ unbecome()
  }
}

object ClientActorByBecome {
  case class Get(key: String)
  case object Connected
  case object Disconnected

  def processMessage(m: String): String = m.reverse
}