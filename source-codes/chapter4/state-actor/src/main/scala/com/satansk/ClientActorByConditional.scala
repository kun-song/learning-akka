package com.satansk

import akka.actor.{Actor, Stash}
import com.satansk.ClientActorByConditional.{Connected, Disconnected, Get}

/**
  * Author: Song Kun
  * Date:   下午11:28 at 18/4/18
  * Email:  satansk@hotmail.com
  */
class ClientActorByConditional extends Actor with Stash {

  var online = false

  override def receive: Receive = {
    case Get(key)     ⇒ if (online) processMessage(key) else stash()
    case Connected    ⇒ online = true; unstashAll()
    case Disconnected ⇒ online = false
  }

  def processMessage(m: String): String = m.reverse
}

object ClientActorByConditional {
  case class Get(key: String)
  case object Connected
  case object Disconnected
}