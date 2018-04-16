package com.akkademaid

/**
  * Author: Kyle Song
  * Date:   PM10:28 at 18/4/15
  * Email:  satansk@hotmail.com
  */
case class ParseArticle(url: String)
case class ParseHtmlArticle(url: String, htmlString: String)
case class HttpResponse(body: String)
case class ArticleBody(url: String, body: String)
