# Failure

8 大误区的第一条就告诉我们：网络不可靠。

除了网络错误，还可能有千千万万其他错误，代码必须能处理这些错误，Akka 本身自带了容错处理机制。

## 1. Isolating failure

分布式应用都应该遵循的错误处理策略为：**隔离错误**。

最著名的例子是船体设计，船体被分成很多互相独立的空间，即使撞到冰山，也只会影响漏水的那些空间，他们被 **隔离** 开了。

### Redundancy

通过 **冗余组件**，消除单点故障，可以保证发生错误时，系统依然可以运行。

## Supervision

Erlang 通过监督机制为 Actor 模型引入 fault tolerance。

监督的核心：

* 将引起错误的组件，与对错误的响应（处理）分开；
  + 父 `Actor` 负责子 `Actor` 错误的处理
* 将可能发生错误的组件，组成便于管理的层次关系；

### 1. Supervision hierarchies

Actor 的监督机制用 Actor 层次结构描述，它与文件系统非常类似：

![img](../images/actor-cengci.png)

Actor 基本分类：

* root Actor
  + 位于 Actor 层次结构的最顶层
  + path: /
* 守护 Actor
  + path: /user
  + 所有用 `ActorSystem.actorOf` 创建的 Actor 都位于 /user 下，即 /user/xxx
* 系统 Actor
  + path: /system
* 临时 Actor
  + path: /temp

使用 `context.actorOf` 创建的 Actor 将位于本 Actor 下面，例如 /user/aaa/xxx。

### 2. Supervision strategies

每个负责监督其他 Actor 的 Actor 都可以定义自己的监督策略：

* Resume
  + 不管不顾，继续处理
* Stop
  + 停止 Actor
* Restart
  + 创建新 Actor，替换老 Actor
* Escalate
  + 将错误上升到高一级监督者

### 3. Defining supervisor strategies

Actor 默认监督策略如下：

* **Exception** in a **running** actor: restart()
* **Error** in a **running** actor: escalate()
* **Exception** in an actor during **initialization**: stop()

>Exception 和 Error 区别？

监督者还有一种行为策略：若 `Actor` 被 kill，其监督者会收到 `ActorKilledException`，该场景的策略为 stop()。

通过重写 `Actor.supervisorStrategy` 可以自定义监督策略：

```Scala
class ManagerActor extends Actor {
  override def receive: Receive = ???

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy() {
      case BrokenPlateException ⇒ Resume
      case DrunkenException     ⇒ Restart
      case TiredException       ⇒ Stop
      case FireError            ⇒ Escalate
      case _                    ⇒ Escalate
    }
}

object ManagerActor {
  case object BrokenPlateException extends Throwable
  case object DrunkenException extends Throwable
  case object FireError extends Error
  case object TiredException extends Throwable
}
```

### 4. Actor lifecycle

`Actor` 声明周期中的钩子方法：

* `preStart`
  + after constructor
* `postStart`
  + before restart
* `preRestart(reason, message)`
  + call `postStop` by default
* `postRestart`
  + call `preStart` by default

`Actor` 生命周期中各个事件、方法的调用顺序如下：

![img](../images/order-of-actor-lifecycle.png)

### 5. Messages in restart, stop

发生异常时，会如何处理消息呢？简单说抛出异常的消息会被 **丢弃**，不会被重新发送。

但可以通过监督策略，在消息 **抛出异常之前**，重新发送失败的消息，可以指定 **重试次数**，也可以指定 **重试时间**，只要任意条件达到，就停止重试，抛出异常：

```Scala
override def supervisorStrategy: SupervisorStrategy =
  OneForOneStrategy(maxNrOfRetries = 10) {
    case BrokenPlateException ⇒ Resume
    case DrunkenException     ⇒ Restart
    case TiredException       ⇒ Stop
    case FireError            ⇒ Escalate
    case _                    ⇒ Escalate
  }
```
* 抛出异常前，重试 10 次；

### 6. Terminating or killing an Actor

有多种方式可停止 Actor：

1. `ActorSystem.stop(actorRef)` 和 `ActorContext.stop(actorRef)`
  + 立即停止 Actor
2. 向 Actor 发送 `PoisonPill` 消息
  + 处理完 `PoisonPill` 消息后停止 Actor
3. 先 Actor 发送 `Kill` 消息
  + 抛出 `ActorKilledException`，由其监督者决定怎么做

### 7. Lifecycle monitoring and DeathWatch

Actor 不仅可以监控 **子 Actor**，也可以监控其他任意 Actor：

1. `context.watch(actorRef)` 注册对 `actorRef` 的监控
2. `context.unwatch(actorRef)` 取消监控

注意，仅监控 **是否终止**，被监控 Actor 终止后，监控者将收到 `Terminated(actorRef)` 消息。

### 8. Safely restarting


