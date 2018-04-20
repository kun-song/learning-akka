# Future 并行编程

`Future` 是一个 monad，可组合性非常强。利用 `Future` 可以很容易地 **并行** 处理集合元素。

假设有 html 格式的文章列表 `articleList`，下面对其并行处理：

```Scala
object FutureParallel {
  import scala.concurrent.ExecutionContext.Implicits.global

  def parseHtmlList(articleList: List[String]): Future[List[String]] =
    Future.sequence(articleList.map { article ⇒
      Future(ArticleParser(article))
    })
}
```

* `List` 中每篇文章的解析都是 **并行** 的；
* `Future.sequence` 将 `List[Future]` 转化为 `Future[List]`；
