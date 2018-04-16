package com.akkademaid

import akka.actor.Actor
import de.l3s.boilerpipe.extractors.ArticleExtractor

/**
  * Author: Kyle Song
  * Date:   PM10:30 at 18/4/15
  * Email:  satansk@hotmail.com
  */
class ParsingActor extends Actor {
  override def receive: Receive = {
    case ParseHtmlArticle(url, html)  ⇒ sender() ! ArticleBody(url, ArticleExtractor.getInstance().getText(html))
    case x                            ⇒ println(s"unknown message: $x")
  }
}
