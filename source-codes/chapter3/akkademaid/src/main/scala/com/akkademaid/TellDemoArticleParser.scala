package com.akkademaid

import java.util.concurrent.TimeoutException

import akka.actor.{Actor, ActorRef, Props, Status}
import akka.util.Timeout
import com.akkademy.messages.{GetRequest, SetRequest}

import scala.concurrent.duration._

/**
  * Author: Kyle Song
  * Date:   PM10:20 at 18/4/16
  * Email:  satansk@hotmail.com
  */
class TellDemoArticleParser(cacheActorPath: String,
                            httpClientActorPath: String,
                            articleParserActorPath: String,
                            implicit val timeout: Timeout) extends Actor {

  val cacheActor = context.actorSelection(cacheActorPath)
  val httpClientActor = context.actorSelection(httpClientActorPath)
  val articleParserActor = context.actorSelection(articleParserActorPath)

  implicit val ec = context.dispatcher  // ExecutionContext

  override def receive: Receive = {
    case msg @ ParseArticle(url)  ⇒

      // 临时 Actor
      val extraActor = buildExtraActor(sender(), url)

      /**
        * 1. 以 tell 方式向 CacheActor 发送 GetRequest 消息，并指定 sender 为 extraActor
        * 2. cacheActor 以 String 响应 extraActor
        */
      cacheActor.tell(GetRequest(url), extraActor)

      /**
        * 1. 以 tell 方式向 HttpClientActor 发送 "test" 消息，并指定 sender 为 extraActor
        * 2. httpClientActor 以 HttpResponse 响应给 extraActor
        */
      httpClientActor.tell(url, extraActor)

      /**
        * 向 extraActor 发送 "timeout" 消息
        */
      context.system.scheduler.scheduleOnce(timeout.duration, extraActor, "timeout")
  }

  /**
    * 1. timeout 优先级最高
    * 2. body 在 ArticleBody 之前（因为查询缓存、解析是同时进行的，哪个先返回，就用哪个）
    */
  private def buildExtraActor(senderRef: ActorRef, url: String): ActorRef =
    context.actorOf(Props(new Actor {
      override def receive: Receive = {
        // 超时，响应 Status.Failure，stop
        case "timeout"              ⇒ senderRef ! Status.Failure(new TimeoutException("timeout!")); context.stop(self)
        // HTML 文章，请求 articleParserActor 解析
        case HttpResponse(body)     ⇒ articleParserActor ! ParseHtmlArticle(url, body)
        // 缓存命中
        case body: String           ⇒ senderRef ! body; context.stop(self)
        // 解析后的文章
        case ArticleBody(url, body) ⇒ cacheActor ! SetRequest(url, body); senderRef ! body; context.stop(self)
        // 缓存未命中
        case _                      ⇒ println("cache missing, ignore it")
      }
    }))
}
