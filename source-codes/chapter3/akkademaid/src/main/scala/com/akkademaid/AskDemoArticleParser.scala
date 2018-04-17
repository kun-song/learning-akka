package com.akkademaid

import akka.pattern._
import akka.actor.{Actor, Status}
import akka.util.Timeout
import com.akkademy.messages.{GetRequest, SetRequest}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Author: Kyle Song
  * Date:   PM10:34 at 18/4/15
  * Email:  satansk@hotmail.com
  */
class AskDemoArticleParser(cacheActorPath: String,
                           httpClientActorPath: String,
                           articleParserActorPath: String,
                           implicit val timeout: Timeout) extends Actor {

  val cacheActor = context.actorSelection(cacheActorPath)
  val httpClientActor = context.actorSelection(httpClientActorPath)
  val articleParserActor = context.actorSelection(articleParserActorPath)

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive: Receive = {
    case ParseArticle(url)  ⇒ {
      /**
        * onComplete 回调函数中引用 senderRef
        */
      val senderRef = sender()

      /**
        * 查询缓存
        */
      val cacheResult = cacheActor ? GetRequest(url)

      val result = cacheResult recoverWith {
        case _: Exception ⇒  // 未命中缓存
          val rawResult = httpClientActor ? url  // 获取 html 格式文章
          rawResult flatMap {
            case HttpResponse(rawArticle) ⇒ articleParserActor ? ParseHtmlArticle(url, rawArticle)  // 获取纯文本
            case _                        ⇒ Future.failed(new Exception("unknown response"))
          }
      }

      result onComplete {
        // 缓存中的文章
        case Success(x: String)       ⇒ senderRef ! x
        // 直接解析的文章
        case Success(x: ArticleBody)  ⇒ cacheActor ! SetRequest(url, x.body); senderRef ! x
        case Failure(e)               ⇒ senderRef ! Status.Failure(e)
        case x                        ⇒ println(s"unknown message: $x")
      }

    }
  }
}
