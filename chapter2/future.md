# Promise、Future、事件驱动

事件/消息驱动的编程模型的基础是 `Future` 和 `Promise`。

## 使用 `Future` 获取响应

要获取 `Actor` 的响应，需要使用 ask pattern：

```Scala
import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class ScalaPongActorSpec extends FunSpecLike with Matchers {

  implicit val system = ActorSystem()
  /**
    * ask/? 使用
    */
  implicit val timeout = Timeout(5 seconds)

  /**
    * 在 ActorSystem 中创建 Actor 实例
    */
  val pongActor: ActorRef = system.actorOf(ScalaPongActor props "Pong Pong Pong")

  describe("Pong Actor") {
    it("should response with Pong Pong Pong") {
      /**
        * 1. ？ from akka.pattern.ask
        * 2. 发送请求，并返回响应
        * 3. ? 有两个 implicit 参数：implicit timeout + implicit ActorRef
        */
      val future = pongActor ? "Ping"

      /**
        * 1. 阻塞，获取 future 内容
        * 2. ? 返回类型为 Future[AnyRef]，将其转换为 String
        */
      val result = Await.result(future.mapTo[String], 1 second)

      result shouldEqual "Pong Pong Pong"
    }

    it("should fail on unknown messages") {
      val future = pongActor ? "unknown message"

      /**
        * Future 返回失败时，将抛出异常
        */
      intercept[Exception] {
        Await.result(future, 1 second)
      }
    }
  }
}
```

* `?` 有个两个 `implicit` 参数：`implicit timeout` + `implicit ActorRef`；
* `?` 返回 `Future[AnyRef]`

## `Future`

一个异步请求可能 **失败** 或 **延迟**，`Future` 在类型中表达失败、延迟的概念。

### `onComplete` 强制处理成功、失败

`Future.onComplete` 接受 `PartialFunction` 作为参数，强制用户处理请求成功、失败的场景：

```Scala
describe("Future Examples") {
  import scala.concurrent.ExecutionContext.Implicits.global

  def askPong(message: String): Future[String] = (pongActor ? message).mapTo[String]

  it("should print response to console") {
    askPong("Ping").onComplete {
      case Success(x) ⇒ println(s"response is $x")
    }
  }

  it("should fail on unknown messages") {
    askPong("unknown message").onComplete {
      case Failure(e) ⇒ println(s"failed exception is $e")
    }
  }
}
```

* `onComplete` 实际会强制用户处理 `Success` 和 `Failure` 两种场景，上面的代码实际会提示模式匹配不完全；

### 成功时，转换结果

处理响应之前，可能需要先对 `Future` 进行类型转换。

#### 同步转换 `map`

同步转换用 `map` 完成：

```Scala
it("should be able to use map") {
  askPong("Ping")
    .map(_.charAt(0))
    .onComplete {
      case Success(x) ⇒ println(s"the first char is $x")
    }
}
```

#### 异步转换 `flatMap`

异步转换用 `flatMap` 完成：

```Scala
it("should be able to use flatMap") {
  askPong("Ping")
    .flatMap(x ⇒ askPong(x))
    .onComplete {
      case Success(x) ⇒ println(s"response is $x")
      case Failure(e) ⇒ println(s"failed exception is $e")
    }
}
```

### 失败时，进行恢复

#### `recover`（类似 `map`）

`recover` 作用类似 `map`，不过是对失败进行转换，而且 `recover` 接受 `PartialFunction` 作为参数：

```Scala
it("should be able to use recover") {
  askPong("error")
    .recover {
      case _: Exception ⇒ "default messsage"
    }
    .onComplete {
      case Success(x) ⇒ println(s"response is $x")
      case Failure(e) ⇒ println(s"failed exception is $e")
    }
}
```

#### `recoverWith`（类似 `flatMap`）

异步调用失败时，常常需要 **调用另外一个异步任务** 来恢复，例如：

* 重试失败的操作；
* 若未命中缓存，则直接调用访问数据库的操作；

用 `recoverWith` 实现重试：

```Scala
it("should be able to use recoverWith") {
  askPong("error")
    .recoverWith {
      case _: Exception ⇒ askPong("Ping")
    }
    .onComplete {
      case Success(x) ⇒ println(s"response is $x")
      case Failure(e) ⇒ println(s"failed exception is $e")
    }
}
```

### 链式操作

`Future` 可以组合多个操作，并且 **仅在末尾** 处理异常：

```Scala
it("should be able to compose operations") {
  askPong("Ping")
    .flatMap(x ⇒ askPong("Ping" + x))
    .recover {
      case _: Exception ⇒ "default message"
    }
    .onComplete {
      case Success(x) ⇒ println(s"response is $x")
      case Failure(e) ⇒ println(s"failed exception is $e")
    }
}
```

调用链中 **任意一环** 出现错误，都会被 **末尾** 的 `recover`/`recoverWith` 捕获处理，因此调用链中可以仅关系 **成功** 的场景，失败场景最后考虑即可。

### 组合 `Future`

可以用 `flatMap` 或 `for` 解析组合多个 `Future`：

```Scala
it("should be able to compose Futures") {
  val f1 = Future(111)
  val f2 = Future(666)

  val sumF: Future[Int] =
    for {
      x ← f1
      y ← f2
    } yield (x + y)
}
```

### 用 `Future.sequence` 处理 `List[Future[A]]`

若要对集合中 **每个元素** 都调用异步方法，则会得到 `List[Future]`：

```Scala
val messages: List[String] = "Ping" :: "Ping" :: "error" :: Nil
val fs: List[Future[String]] = messages.map(askPong)
```

直接处理 `List[Future[String]]` 比较繁琐，而且 `List[Future]` 也不是我们想要的结果，我们更希望得到 `Future[List]`，可以用 `Future.sequence` 进行转换，但直接使用 `sequence` 时，若 `List[Future]` 中有任何一个 `Future` 失败，则转换后的 `Future` 也是失败的，因此调用 `sequence` 之前，先恢复可能存在的错误：

```Scala
val result: Future[List[String]] =
  Future.sequence(fs.map(_.recover {
    case _: Exception ⇒ "default response"
  }))
```

完整代码如下：

```Scala
it("should be able to process List of Futures") {
  val messages: List[String] = "Ping" :: "Ping" :: "error" :: Nil
  val fs: List[Future[String]] = messages.map(askPong)

  val result: Future[List[String]] =
    Future.sequence(fs.map(_.recover {
      case _: Exception ⇒ "default response"
    }))

  result.onComplete {
    case Success(x) ⇒ println(s"response is $x")
    case Failure(e) ⇒ println(s"failed exception is $e")
  }
}
```
