package com.satansk

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{RoundRobinGroup, RoundRobinPool}

/**
  * Author: Song Kun
  * Date:   上午8:05 at 18/4/20
  * Email:  satansk@hotmail.com
  */
class Router(actors: List[ActorRef]) extends Actor {

  val router = context.system.actorOf(
    Props(classOf[ArticleParseActor])
      .withRouter(new RoundRobinPool(8).withSupervisorStrategy(myStrategy)))


  override def receive: Receive = ???
}

