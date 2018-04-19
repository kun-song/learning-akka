# State

`Actor` 可以存储状态，且 **不同状态** 会导致 **不同行为**。

改变 `Actor` 行为的方式：

1. 基于 `Actor` 状态的条件语句
2. Hotswap
  + `become()`
  + `unbecome()`
3. Finite State Machine
  + 有限状态机


#### 状态转移

`Actor` 状态的常见例子是 **服务是否离线**。

`Actor` 常常处于一种 **无法处理消息** 的状态，如 remote db 断连时，client Actor 就无法处理消息；前面的例子中 client 重新连接成功之前，client 会 **抛弃** 所有消息。

另一种做法是 client 连接成功前，使用 `stash` 暂存收到的消息，连接成功后，`unstash` 这些消息重新处理。

>要使用 `stash` 和 `unstash`，需要混入 `Stask` 特质。

## 1. 条件语句

将状态存储在 `Actor` 中，使用条件语句根据状态，来决定 `Actor` 的行为。

对于 client 的例子，可以在 `Actor` 中存储一个布尔变量，表示该 `Actor` 是否连接到数据库：

```Scala
class ClientActorByConditional extends Actor with Stash {

  var online = false

  override def receive: Receive = {
    case Get(key)     ⇒ if (online) processMessage(key) else stash()
    case Connected    ⇒ online = true; unstashAll()
    case Disconnected ⇒ online = false
  }

  def processMessage(m: String): String = m.reverse
}

object ClientActorByConditional {
  case class Get(key: String)
  case object Connected
  case object Disconnected
}
```

条件语句是过程式风格，并非最佳。

## 2. Hotswap: become & unbecome

Akka 提供 `become` 和 `unbecome` 用于 **管理行为**，这两个方法位于 `Actor` 的 `context` 中：

* `become(PartialFunction behavior)`
  + 将 `Actor` 行为从 `receive` 方法定义，改为 `become` 参数中的 `PartialFunction`
* `unbecome`
  + 将 `Actor` 行为恢复到 `receive` 定义的默认行为

>`PartialFunction` 定义了 `Actor` 的行为，决定收到消息时，如何处理消息。

使用 `become` 和 `unbecome` 重写 client 例子：

```Scala
class ClientActorByBecome extends Actor with Stash {
  import context._
  
  /**
    * 离线状态（默认）
    */
  override def receive: Receive = {
    case Get(key)   ⇒ stash()
    case Connected  ⇒ become(online); unstashAll()
  }

  /**
    * 在线状态
    */
  def online: Receive = {
    case Get(key)     ⇒ ClientActorByBecome processMessage key
    case Disconnected ⇒ unbecome()
  }
}

object ClientActorByBecome {
  case class Get(key: String)
  case object Connected
  case object Disconnected

  def processMessage(m: String): String = m.reverse
}
```

`become` `unbecome` 相比条件语句，显式定义 **不同状态** 的 **不同行为**，可读性高：

* `receive` 是离线状态的行为
* `online` 是在线状态的行为

可以定义 **任意数量** 的 `Receive`，并互相切换。

## stash 泄漏

前面两个例子都有一个问题：若长时间没收到 `Connected` 消息，或者根本无法收到 `Connected` 消息，`Actor` 会不断 stash 消息，最终导致：

* 内存溢出
* 邮箱满

因此使用 stash 时，最好定义 stash 执行的 **最长时间**，或 stash 消息的 **最大数量**。

可以在 `preStart` 中用 `scheduler` 调度一个超时消息，该超时消息仅在 **离线状态** 中处理，**在线状态** 忽略：

```Scala
class ClientActorTimeout(timeout: Timeout) extends Actor with Stash {
  import context._

  /**
   * 调度一个超时消息
   */
  override def preStart(): Unit = {
    system.scheduler.scheduleOnce(timeout.duration, self, CheckConnected)
  }

  /**
    * 离线状态（默认）
    */
  override def receive: Receive = {
    case Get(key)       ⇒ stash()
    case Connected      ⇒ become(online); unstashAll()

    /**
      * 仅离线状态需要处理 CheckConnected 消息
      */
    case CheckConnected ⇒ throw new TimeoutException
  }

  /**
    * 在线状态，忽略 CheckConnected
    */
  def online: Receive = {
    case Get(key)     ⇒ ClientActorTimeout processMessage key
    case Disconnected ⇒ unbecome()
  }
}

object ClientActorTimeout {
  case class Get(key: String)
  case object Connected
  case object Disconnected
  case object CheckConnected

  def processMessage(m: String): String = m.reverse
}
```

## 3. Finite State Machine 有限自动机

相比热交换，FSM 更加重量级，需要更多代码。

前面介绍 8 大误区时讲到，每个请求都有时延，因此尽量 **减少请求数量**，增加每个请求中的消息个数，以减少时延的影响。

对于 db 例子，可以让 remote db 接受一个查询列表，这样在每次请求中就可以包含多个消息了：

```Scala
remoteActor ? List(
  SetRequest(id, user, sender),
  SetRequest(id, profile, sender),
  ...
)
```

将该 API 直接暴露给用户，其实用户使用并不方便。

可在 `ClientActor` 中再创建一个 `Actor`，该 `Actor` 累计收集消息，当累积到一定数量 or 收到 `Flush` 消息时，就将 **消息列表** 发送到 remote db。

### FSM 类型

FSM 有两个类型参数，分别是 state 和 container，要使用 FSM，首先需要定义状态 & 容器。

#### 定义状态

现在 `ClientActor` 离线时，不使用 `stash`，而是将消息存储在另一个 `Actor` 中，因此有 4 种状态：

* Disconnected
  + 离线
  + 队列无消息
* Disconnected and Pending
  + 离线
  + 队列有消息
* Connected
  + 在线
  + 队列无消息
* Connected and Pending
  + 在线
  + 队列有消息

使用 `case object` 定义它们：

```Scala
object ActorFSM {
  sealed trait State
  
  case object Disconnected extends State
  case object Connected extends State
  case object ConnectedAndPending extends State
}
```

#### 定义容器 