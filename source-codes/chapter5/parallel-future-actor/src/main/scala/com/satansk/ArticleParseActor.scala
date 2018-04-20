package com.satansk

import akka.actor.Actor

/**
  * Author: Song Kun
  * Date:   下午11:24 at 18/4/19
  * Email:  satansk@hotmail.com
  */
class ArticleParseActor extends Actor {
  import ArticleParseActor._

  override def receive: Receive = {
    // 调用 ArticleParser.apply 解析 html
    case ParseArticle(html) ⇒ sender() ! ArticleParser(html)
  }
}

object ArticleParseActor {
  // 消息定义
  case class ParseArticle(html: String)
}
