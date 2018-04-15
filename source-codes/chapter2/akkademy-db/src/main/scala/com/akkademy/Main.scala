package com.akkademy

import akka.actor.{ActorSystem, Props}

/**
  * Author: Kyle Song
  * Date:   PM4:46 at 18/4/15
  * Email:  satansk@hotmail.com
  */
object Main extends App {
  val system = ActorSystem("akkademy")
  system.actorOf(Props[AkkademyDb], "akkademy-db")
}
