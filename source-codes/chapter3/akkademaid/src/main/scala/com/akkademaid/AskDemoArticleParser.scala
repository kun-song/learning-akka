package com.akkademaid

import akka.pattern._
import akka.actor.Actor
import akka.util.Timeout
import com.akkademy.messages.GetRequest

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
      val senderRef = sender()
      /**
        * 查询缓存
        */
      val cacheResult = cacheActor ? GetRequest(url)

      
    }

  }
}
