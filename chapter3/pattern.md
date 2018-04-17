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
