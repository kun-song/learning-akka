# 消息模式

Akka 有 4 种核心的消息模式，用于在 `Actor` 之间发送消息：

* `ask`
  + 发送消息，返回 `Future`
* `tell`
* `forward`
* `pipe`

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

通过 **消息传递** 可以协调多个 `Actor` 之间的行为，


