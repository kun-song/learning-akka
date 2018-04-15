# 练习

编写一个 `Actor`，负责将字符串翻转，当接受到未知类型时直接报错，编写单元测试，覆盖成功、失败的场景，最后向 `Actor` 发送字符串列表，使用 `sequence` 进行处理。

`Actor` 实现：

```Scala
class ReverseActor extends Actor {
  override def receive: Receive = {
    case s: String  ⇒ sender() ! s.reverse
    case x          ⇒ sender() ! Status.Failure(new Exception(s"$x is not a String!"))
  }
}
```

单元测试：

```Scala
package com.reverse

import akka.pattern._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class ReverseActorSpec extends FunSpecLike with Matchers {
  implicit val system = ActorSystem()
  implicit val timeout = Timeout(5 seconds)

  val reverseActor: ActorRef = system.actorOf(Props[ReverseActor])

  def reverse(str: String): Future[String] = (reverseActor ? str).mapTo[String]

  describe("ReverseActor") {
    it("should be able to reverse a String") {
      Await.result(reverse("Hello"), 1 second) shouldEqual "olleH"
    }

    it("should throw an Exception when parsed non String") {
      intercept[Exception] {
        Await.result(reverseActor ? 666, 1 second)
      }
    }

    it("should be able to use Future.sequence") {
      val xs = "Hello" :: "World" :: Nil
      val futureList: List[Future[String]] = xs map reverse
      val listFuture: Future[List[String]] = Future.sequence(futureList)
      Await.result(listFuture, 1 second) shouldEqual "olleH" :: "dlroW" :: Nil
    }
  }
}
```