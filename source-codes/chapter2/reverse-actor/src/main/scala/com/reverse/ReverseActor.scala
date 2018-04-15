package com.reverse

import akka.actor.{Actor, Status}

/**
  * Author: Kyle Song
  * Date:   PM9:15 at 18/4/15
  * Email:  satansk@hotmail.com
  */
class ReverseActor extends Actor {
  override def receive: Receive = {
    case s: String  ⇒ sender() ! s.reverse
    case x          ⇒ sender() ! Status.Failure(new Exception(s"$x is not a String!"))
  }
}
