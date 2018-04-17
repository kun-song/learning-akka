# Pipe

一个常见场景：`Actor` 中有 `Future`，目标是把 `Future` 的计算结果发送回 sender Actor。

前面的展示过一种实现：

```Scala
val senderRef = sender()

future.map(x => senderRef ! ActorRef.noSender)
```

必须在 **先前线程** 执行 `sender()` 并保存其值，然后在 `Future` 回调函数 `map` 中引用 `senderRef`，下面是一种常见错误做法：

```Scala
future.map(x => sender() ! ActorRef.noSender)
```

错误原因：

* `Future` 的回调函数，例如 `map` 是在 **单独线程** 中执行的，在另外线程中执行 `sender()` 无法获取 `ActorRef`；

使用 pipe 可以简化上面的处理：

```Scala
future pipeTo sender()

pipe(future) to sender()
```

* 此时 `sender()` 在当前线程中执行；
* `pipe` 接受 `Future`，获取其计算结果，将结果发送到指定的 `ActorRef`，本例中是 `sender()` 的计算结果；
