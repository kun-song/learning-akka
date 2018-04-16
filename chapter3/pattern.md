# 消息模式

Akka 有 4 种核心的消息模式，用于在 `Actor` 之间发送消息：

* `ask`
  + 发送消息给 `Actor`，返回表示响应的 `Future`
  + receiver `Actor` 完成任务后，通过完成 `Future` 来完成响应
  + receiver `Actor` 不会向 sender `Actor` 的邮箱发送消息
* `tell`
  + 发送消息给 `Actor`
  + 通过 `sender()` 发送消息回 sender `Actor`
* `forward`
  + 转发消息
  + `sender()` 将获取 original message sender `Actor`
* `pipe`
  + 将 `Future` 结果发送回 `sender()` 或其他 `Actor`

## ask 模式

ask 模式生成一个 `Future`，表示 `Actor` 返回的响应，ask 模式常用于 `ActorSystem` **外部普通对象** 与 `Actor` 对象通信。

`ActorA` 使用 ask 对 `ActorB` 发起请求时，Akka 会在 `ActorSystem` 中创建一个 **临时 `Actor`**，在 `ActorB` 中执行 `sender()` 获取的其实就是 **临时 `Actor`**，当 `ActorB` 接收到请求，处理完成并响应时，**临时 `Actor`** 会完成 ask 生成的 `Future`。

ask 模式必须设置 **超时参数**，Scala 中使用 `implicit val` 隐式传入，非常简洁：

```Scala
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

implicit val timeout = Timeout(1 second)

val future = actorRef ? "message"
```

### 使用 ask 进行设计

通过 **消息传递** 可以协调多个 `Actor` 之间的行为，若有如下文章解析业务：

1. 使用 `CacheActor` 检查该 url 指定文章是否被缓存；
2. 若未命中缓存，则请求 `HttpClientActor` 下载该 url 的 html 格式文章，然后请求 `ArticleParserActor` 解析 html，返回纯文本；
3. 缓存解析结果，并相应用户；

下面使用 ask 模式设计的代码，并非最优，仅作为一个起点：

```Scala
package com.akkademaid

import akka.pattern._
import akka.actor.{Actor, Status}
import akka.util.Timeout
import com.akkademy.messages.{GetRequest, SetRequest}

import scala.concurrent.Future
import scala.util.{Failure, Success}

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

      val cacheResult = cacheActor ? GetRequest(url)

      /**
        * 1. recoverWith flatMap 很类似
        * 2. 用 recoverWith 和 flatMap 表达业务逻辑
        */
      val result = cacheResult.recoverWith {
        case _: Exception ⇒
          val rawResult = httpClientActor ? url
          rawResult.flatMap {
            case HttpResponse(rawArticle) ⇒ articleParserActor ? ParseHtmlArticle(url, rawArticle)
            case _                        ⇒ Future.failed(new Exception("unknown response"))
          }
      }

      result.onComplete {
        case Success(x: String)      ⇒ println(s"cached result: $x"); senderRef ! x

        /**
          * 1. 缓存 cacheActor ! SetRequest(url, x)
          * 2. 响应 senderRef ! x
          */
        case Success(x: ArticleBody) ⇒ println(s"parsed result: $x"); cacheActor ! SetRequest(url, x); senderRef ! x
        case Failure(e)              ⇒ senderRef ! Status.Failure(e)
        case x                       ⇒ println(s"unknown message: $x")
      }

    }
  }
}
```

* 构造函数包含 `CacheActor` `HttpClientActor` 和 `ArticleParserActor` 的路径，这是 **依赖注入**；
* `AskDemoArticleParser` 内部通过 `actorSelection` 查找 `Actor`
* 依赖注入：测试环境 `CacheActor` 是本地的，生产环境 `CacheActor` 是远程的，非常方便；

**注意**：

* 使用 ask 设计非常简单，但有几处要注意的问题；
* 有时 tell 比 ask 更好；

### ask 注意点



