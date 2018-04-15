# 剖析 Actor

## 响应消息

```Scala
class ScalaPongActor extends Actor {
  override def receive: Receive = {
    case "Ping" ⇒ sender() ! "Pong"
    case _      ⇒ sender() ! Status.Failure(new Exception("unknown message!"))
  }
}
```

* 定义 Actor，只要继承 `Actor`，并重写 `receive` 方法即可；
* `receive` 返回类型为 `Receiver`，进而为 `PartionalFunction[scala.Any, scala.Unit]`，可以不覆盖所有消息类型；
* `sender()` 可获取 **发送者** 的 `ActorRef`；
* 通过 `tell` 方法发送响应；

`!` 定义如下：

```Scala
/**
  * Sends a one-way asynchronous message. E.g. fire-and-forget semantics.
  * <p/>
  *
  * If invoked from within an actor then the actor reference is implicitly passed on as the implicit 'sender' argument.
  * <p/>
  *
  * This actor 'sender' reference is then available in the receiving actor in the 'sender()' member variable,
  * if invoked from within an Actor. If not then no sender is available.
  * <pre>
  *   actor ! message
  * </pre>
  * <p/>
  */
def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit
```

Actor 中有一个 implicit 变量 self，是指向本身的 `ActorRef`，因此在 **Actor 内部** 执行 `tell`，其发送者永远是本人：

```Scala
/**
  * The 'self' field holds the ActorRef for this actor.
  * <p/>
  * Can be used to send messages to itself:
  * <pre>
  * self ! message
  * </pre>
  */
implicit final val self = context.self //MUST BE A VAL, TRUST ME
```

因此：

* 在 Actor 内部执行 `!`，则发送者默认为本人；
* 在 Actor 外部执行 `!`，则默认 `noSender`；

>`Status.Failure`
>
>Actor 本身在 **任何情况** 下都 **不会反回 `Failure`**，只能主动发送失败给 sender，返回 `Failure` 会导致 sender 的 `Future` 被标记为失败。

## 创建 Actor

在 Akka 中，我们永远不直接使用 `Actor` 实例：

* 不会直接调用 `Actor` 的方法
* 不会直接访问 `Actor` 的成员变量
* 更不会直接修改 `Actor` 的状态

唯一的通信方式为：消息，通过发送消息来：

* 调用 `Actor` 方法
* 获取、修改 `Actor` 状态

通过消息机制，Actor 被非常好的封装起来，用户也 **不需要** 使用 Actor 实例，Akka 使用 `ActorRef` 作为指向 `Actor` 实例的 **引用**，利用 `ActorRef` 将 `Actor` 彻底封装，只留下消息这华山一条路。

`ActorSystem` 包含所有 `Actor`，因此需要在 `ActorSystem` 中创建 `Actor` 实例：

```Scala
object Test extends App {
  implicit val system = ActorSystem()

  val pongActor: ActorRef = system.actorOf(Props[classOf[ScalaPongActor]])
}
```

* `actorOf` 接受 `Props` 实例作为参数，创建 `Actor`，返回 `ActorRef`；

### `Props` 实例

为封装创建好的 `Actor` 实例，使其无法被外界访问，`Props` 实例会携带创建 `Actor` 实例需要的内容：

* `Actor` 类型
* 变长的参数列表（若 `Actor` 构造函数有参数，则有，否则，无）

例如：

```Scala
Props[classOf[PongActor, a, b, c]]
```

* 要创建的 `Actor` 类型为 `PongActor`；
* `PongActor` 的构造函数需要 3 个参数；

当 `Actor` 构造函数有参数时，推荐使用工厂方法来创建 `Props` 实例，以集中管理 `Props` 实例的创建。例如若 `PongActor` 定义如下：

```Scala
class ScalaPongActor(response: String) extends Actor {
  override def receive: Receive = {
    case "Ping" ⇒ sender() ! response
    case _      ⇒ sender() ! Status.Failure(new Exception("unknown message!"))
  }
}
```

则可以在 `ScalaPongActor` 伴生对象中定义创建 `Props` 实例的工厂方法：

```Scala
object ScalaPongActor {
  /**
    * 1. 若 Actor 构造函数有参数，推荐使用工厂方法创建 Props
    * 2. 使用工厂方法可 集中 管理 Props 对象的创建
    */
  def props(response: String): Props =
    Props(classOf[ScalaPongActor], response)
}
```

### `actorOf` & `actorSelection`

#### `actorOf` 直接创建 `Actor`

`actorOf` 根据传入的 `Props` 实例，创建 `Actor` 实例，并返回指向该 `Actor` 实例的 `ActorRef`。

#### `actorSelection` 查找已存在的 `Actor`

每个创建好的 `Actor` 实例都有一个 `path` 路径：

```Scala
// akka://default/user/$a
println(pongActor.path)
```

根据 `Actor` 是在本地还是远端服务器，`path` 分为两类：

* 本地路径：`akka://default/user/$a`
* 远程路径：`akka.tcp://my-sys@remote-host:port/user/$a`

根据 `path`，可以用 `actorSelection` 获取指向该 `Actor` 的 `ActorSelection` 实例：

```Scala
val x: ActorSelection = system.actorSelection(pongActor.path)
```

`ActorSelection` 使用与 `ActorRef` 类似。

