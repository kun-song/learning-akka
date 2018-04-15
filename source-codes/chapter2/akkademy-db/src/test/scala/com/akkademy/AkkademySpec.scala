package com.akkademy

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.akkademy.messages.SetRequest
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.duration._

/**
  * Author: Kyle Song
  * Date:   PM9:31 at 18/4/14
  * Email:  satansk@hotmail.com
  */
class AkkademySpec extends FunSpecLike with Matchers {

  implicit val system = ActorSystem()
  implicit val timeout = Timeout(5 seconds)

  describe("akkademy") {
    describe("given SetRequest") {
      it("should place key-value into map") {
        /**
          * TestActorRef 有 implicit ActorSystem 参数
          */
        val actorRef = TestActorRef(new AkkademyDb)

        /**
          * TestActorRef 的 ! 是同步方法
          */
        actorRef ! SetRequest("key", "value")

        /**
          * 通过 TestActorRef 访问真实的 AkkademyDb 对象
          */
        val akkademyDb = actorRef.underlyingActor
        akkademyDb.map.get("key") shouldEqual Some("value")
      }
    }
  }

}
