package com.satansk

import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Author: Kyle Song
  * Date:   AM9:54 at 18/4/15
  * Email:  satansk@hotmail.com
  */
class ScalaPongActorSpec extends FunSpecLike with Matchers {

  implicit val system = ActorSystem()
  implicit val timeout = Timeout(5 seconds)

  val pongActor: ActorRef = system.actorOf(ScalaPongActor props "Pong Pong Pong")

  describe("Future Examples") {
    import scala.concurrent.ExecutionContext.Implicits.global

    def askPong(message: String): Future[String] = (pongActor ? message).mapTo[String]

    it("should print response to console") {
      askPong("Ping").onComplete {
        case Success(x) ⇒ println(s"response is $x")
      }
    }

    it("should fail on unknown messages") {
      askPong("unknown message").onComplete {
        case Failure(e) ⇒ println(s"failed exception is $e")
      }
    }

    it("should be able to use map") {
      askPong("Ping")
        .map(_.charAt(0))
        .onComplete {
          case Success(x) ⇒ println(s"the first char is $x")
        }
    }

    it("should be able to use flatMap") {
      askPong("Ping")
        .flatMap(x ⇒ askPong(x))
        .onComplete {
          case Success(x) ⇒ println(s"response is $x")
          case Failure(e) ⇒ println(s"failed exception is $e")
        }
    }

    it("should be able to use recover") {
      askPong("error")
        .recover {
          case _: Exception ⇒ "default messsage"
        }
        .onComplete {
          case Success(x) ⇒ println(s"response is $x")
          case Failure(e) ⇒ println(s"failed exception is $e")
        }
    }

    it("should be able to use recoverWith") {
      askPong("error")
        .recoverWith {
          case _: Exception ⇒ askPong("Ping")
        }
        .onComplete {
          case Success(x) ⇒ println(s"response is $x")
          case Failure(e) ⇒ println(s"failed exception is $e")
        }
    }

    it("should be able to compose operations") {
      askPong("Ping")
        .flatMap(x ⇒ askPong("Ping" + x))
        .recover {
          case _: Exception ⇒ "default message"
        }
        .onComplete {
          case Success(x) ⇒ println(s"response is $x")
          case Failure(e) ⇒ println(s"failed exception is $e")
        }
    }

    it("should be able to compose Futures") {
      val f1 = Future(111)
      val f2 = Future(666)

      val sumF: Future[Int] =
        for {
          x ← f1
          y ← f2
        } yield (x + y)
    }

    it("should be able to process List of Futures") {
      val messages: List[String] = "Ping" :: "Ping" :: "error" :: Nil
      val fs: List[Future[String]] = messages.map(askPong)

      val result: Future[List[String]] =
        Future.sequence(fs.map(_.recover {
          case _: Exception ⇒ "default response"
        }))

      result.onComplete {
        case Success(x) ⇒ println(s"response is $x")
        case Failure(e) ⇒ println(s"failed exception is $e")
      }
    }
  }
}
