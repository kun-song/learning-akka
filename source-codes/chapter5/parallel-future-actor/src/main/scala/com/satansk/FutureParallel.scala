package com.satansk

import scala.concurrent.Future

/**
  * Author: Song Kun
  * Date:   下午10:43 at 18/4/19
  * Email:  satansk@hotmail.com
  */
object FutureParallel {
  import scala.concurrent.ExecutionContext.Implicits.global

  def parseHtmlList(articleList: List[String]): Future[List[String]] =
    Future.sequence(articleList.map { article ⇒
      Future(ArticleParser(article))
    })
}
