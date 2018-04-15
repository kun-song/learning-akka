package com.satansk

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props, Status}

/**
  * Author: Kyle Song
  * Date:   PM10:40 at 18/4/14
  * Email:  satansk@hotmail.com
  */
class ScalaPongActor(response: String) extends Actor {
  override def receive: Receive = {
    case "Ping" ⇒ sender() ! response
    case _      ⇒ sender() ! Status.Failure(new Exception("unknown message!"))
  }
}

object ScalaPongActor {
  /**
    * 1. 若 Actor 构造函数有参数，推荐使用工厂方法创建 Props
    * 2. 使用工厂方法可 集中 管理 Props 对象的创建
    */
  def props(response: String): Props =
    Props(classOf[ScalaPongActor], response)
}

object Test extends App {
  implicit val system = ActorSystem()

  val pongActor: ActorRef = system.actorOf(ScalaPongActor props "xxxs")

  val x: ActorSelection = system.actorSelection(pongActor.path)

  println(pongActor.path)
}