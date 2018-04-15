# 客户端

## 依赖 & 配置

添加消息依赖：

```Scala
libraryDependencies ++= Seq(
  "org.scalactic"     %% "scalactic" % "3.0.5",
  "org.scalatest"     %% "scalatest" % "3.0.5" % "test",

  "com.akkademy-db"   %% "akkademy-db" % "0.1"
)
```

* 前提：`publishLocal` akkademy-db，否则无法解析依赖；

**注意**：同样需要添加 `src/resources/application.conf`：

```Scala
akka {
  actor {
    provider = remote
  }
}
```

## 客户端代码

代码很简单，首先用 `actorSelection` 获取远程 `Actor`，然后使用 ask pattern 向该 `Actor` 发送消息，并返回 `Future`：

```Scala
package com.akkademy.client

import akka.pattern._
import akka.actor.ActorSystem
import akka.util.Timeout
import com.akkademy.messages.{GetRequest, SetRequest}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class Client(remoteAddress: String) {

  private implicit val timeout = Timeout(5 seconds)
  private implicit val system = ActorSystem("LocalSystem")

  /**
    * 通过 actorSelection 查找远程 Actor
    */
  private val remoteDb = system.actorSelection(s"akka.tcp://akkademy@$remoteAddress/user/akkademy-db")

  /**
    * 异步函数
    */
  def set(key: String, value: Object): Future[Any] = remoteDb ? SetRequest(key, value)
  def get(key: String): Future[Any] = remoteDb ? GetRequest(key)

}
```

集成测试，需要保持数据库运行：

```Scala
package com.akkademy.client

import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class ClientSpec extends FunSpecLike with Matchers {

  val client = new Client("127.0.0.1:2552")

  describe("akkademyDb client") {
    it("should set a value") {

      client.set("a", "666")

      val resultF = client.get("a")
      val result = Await.result(resultF, 1 second)

      result shouldEqual "666"
    }
  }
}
```
