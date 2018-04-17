# Forward

使用 tell 若不指定 sender，则默认为 current Actor，forward 却不同，它的 reply address 为 **original sender**。

代理模式下 forward 非常有用，即：

1. ActorA 发送消息给 WorkActor1
2. WorkActor1 将消息 forward 给 WorkActor2 处理
3. WorkActor2 将消息 forward 给 RealWork 处理

最后是 RealWork 完成了消息指定的任务，而且处理结果需要发送给 ActorA 使用，此时 forward 非常适合。

forward 非常简单，用 tell 也能实现同样的语义：只要手动为 tell 指定 **reply address** 为 `sender()` 即可：

```Scala
actor forward message

actor.tell(message, sender())
```

forward 相比 tell 的优势是：语义更加清晰。
