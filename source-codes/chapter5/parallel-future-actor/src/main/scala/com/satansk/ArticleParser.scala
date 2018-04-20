package com.satansk

import de.l3s.boilerpipe.extractors.ArticleExtractor

/**
  * Author: Song Kun
  * Date:   下午10:28 at 18/4/19
  * Email:  satansk@hotmail.com
  */
object ArticleParser {
  def apply(html: String): String = ArticleExtractor.INSTANCE.getText(html)
}
