# Actor + Router 并行编程

使用 `Actor` 可以实现与 `Future` 效果完全相同的并行编程，不过一般需要更多代码。

首先创建执行解析任务的 `Actor`：

```Scala
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
```

## Router

在 Akka 中，Router 负责：

* 负载均衡
* 路由

有两种创建 Router 的方式：

1. 使用已经存在的 a group of actors
2. 让 Router 自己创建 a pool of actors

Router 将 **消息分发** 到 group/pool 中的 `Actor` 上，从而实现并行计算。

假设已有 Actor 列表 `actors`，则可以 actor group 方式创建 router：

```Scala
val router = context.system.actorOf(new RoundRobinGroup(actors.map(_.path)).props())
```

若没有已存在的 Actor，则可以 actor pool 方式创建：

```Scala
val router = context.system.actorOf(
  Props(classOf[ArticleParseActor])
    .withRouter(new RoundRobinPool(8)))
```

## 广播

无论通过 actor pool 还是 actor group 创建 router，都可以通过广播将消息发送给 **所有 Actor**：

```Scala
router ! akka.routing.Broadcast(message)
```

## 监督 routee 对象

使用 actor pool 创建 router 时，pool 中新创建的 actor 将成为 router 的 **子节点**，router 可以定义监督它们的策略：

```Scala
val router = context.system.actorOf(
  Props(classOf[ArticleParseActor])
    .withRouter(new RoundRobinPool(8).withSupervisorStrategy(myStrategy)))
```

而使用 actor group 创建时，因为 group 中的 routee **已经存在**，所以 router 无法监督它们。

