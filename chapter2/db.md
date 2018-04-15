# AkkademyDb & 客户端

数据库与其客户端之间需要 **共享** 消息，在 client 中添加对 AkkademyDb 的依赖实现。

## AkkademyDb

### 消息

因为消息需要通过网络传输，因此必须 **可序列化**，Scala 的 `case class` 默认就是可序列化的，非常适合定义消息：

```Scala
case class SetRequest(key: String, value: Object)
case class GetRequest(key: String)
case class KeyNotFoundException(key: String) extends Exception
```

>消息两大要求：1. 可序列化；2. 不可变。

### `Actor` 实现

实现 `Actor` 对这 3 种消息的响应：

```Scala
class AkkademyDb extends Actor {

  val map = new mutable.HashMap[String, Object]
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case SetRequest(key, value) ⇒
      log.info(s"received SetRequest = key: $key, value: $value")
      map.put(key, value)
      sender() ! Status.Success       // 成功响应
    case GetRequest(key)        ⇒
      log.info(s"received GetRequest = key: $key")
      map.get(key) match {
        case Some(v)  ⇒ sender() ! v  // 响应 value 值
        case None     ⇒ sender() ! Status.Failure(KeyNotFoundException(key))  // 失败响应
      }
    case unknown                ⇒
      log.info(s"received unknown message = $unknown")
      sender() ! Status.Failure(new ClassNotFoundException)  // 失败响应
  }
}
```

* 成功：`sender() ! Status.Success`
* 失败：`sender() ! Status.Failure(xxx)`

### 远程访问

上面定义的 `AkkademyDb` Actor 必须支持 **远程访问**，首先添加依赖：

```Scala
"com.typesafe.akka" %% "akka-remote" % "2.5.12"
```

然后在 `src/main/resources` 下添加 `application.conf` 配置文件：

```Scala
akka {
  actor {
    provider = remote
  }
  remote {
    enable-transports = ["akka.remote.netty.tcp"]
    netty.tcp {
      hostname = "127.0.0.1"
      port = 2552
    }
  }
}
```

* Akka 会自动解析 `application.conf` 文件；

### 启动入门

最后需要定义 `main` 函数，来创建 `ActorSystem`，创建 `Actor`，启动整个系统：

```Scala
object Main extends App {
  val system = ActorSystem("akkademy")
  system.actorOf(Props[AkkademyDb], "akkademy-db")
}
```

* 为 `AkkademyDb` Actor 起了一个名字，便于客户端查找该 Actor；

### 发布消息

首先在 `build.sbt` 中添加基本信息：

```Scala
name := "akkademy-db"
organization := "com.akkademy-db"
version := "0.1"
```

其次，正规的做法是，将消息放在单独的库中独立打包发布，这里为了简单放到了 db 工程中，因此需要在 `build.sbt` 中禁止对 `application.conf` 发布，以防止别有用心的人获取 Actor 的配置信息：

```Scala
/**
  * 防止发布 application.conf
  */
mappings in (Compile, packageBin) ~= { _.filterNot {
  case (_, name)  ⇒ Seq("application.conf").contains(name)
}}
```

最后在 sbt shell 中输入 `publishLocal` 即可将其发布在本地。

### 启动

