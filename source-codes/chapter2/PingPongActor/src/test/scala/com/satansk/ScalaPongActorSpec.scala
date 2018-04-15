package com.satansk

import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Author: Kyle Song
  * Date:   AM9:54 at 18/4/15
  * Email:  satansk@hotmail.com
  */
class ScalaPongActorSpec extends FunSpecLike with Matchers {

  implicit val system = ActorSystem()
  /**
    * ask/? 使用
    */
  implicit val timeout = Timeout(5 seconds)

  /**
    * 在 ActorSystem 中创建 Actor 实例
    */
  val pongActor: ActorRef = system.actorOf(ScalaPongActor props "Pong Pong Pong")

  describe("Pong Actor") {
    it("should response with Pong Pong Pong") {
      /**
        * 1. ？ from akka.pattern.ask
        * 2. 发送请求，并返回响应
        * 3. ? 有两个 implicit 参数：implicit timeout + implicit ActorRef
        */
      val future = pongActor ? "Ping"

      /**
        * 1. 阻塞，获取 future 内容
        * 2. ? 返回类型为 Future[AnyRef]，将其转换为 String
        */
      val result = Await.result(future.mapTo[String], 1 second)

      result shouldEqual "Pong Pong Pong"
    }

    it("should fail on unknown messages") {
      val future = pongActor ? "unknown message"

      /**
        * Future 返回失败时，将抛出异常
        */
      intercept[Exception] {
        Await.result(future, 1 second)
      }
    }
  }

}
