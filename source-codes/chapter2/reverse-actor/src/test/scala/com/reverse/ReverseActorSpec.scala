package com.reverse

import akka.pattern._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Author: Kyle Song
  * Date:   PM9:19 at 18/4/15
  * Email:  satansk@hotmail.com
  */
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
