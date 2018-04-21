# Dispatcher 隔离性能风险

## Dispatcher 介绍

Dispatcher 拥有线程资源，这些线程负责：

* 处理 Actor 消息
* 完成 `Future` 回调函数

`Actor` 和 `Future` 中指定的 **所有任务**，实际上都是由 dispatcher 完成的！

`Future` 所有 API 都需要一个 `ExecutionContext` 隐式参数，而 Akka 的 `Dispatcher` 扩展了 `ExecutionContext`，因此可以用作 `Future` 的底层执行。

Akka 的 Dispatcher 实现了 `scala.concurrent.ExecutionContext` 接口，该接口扩展了以下两个接口：

* `java.util.concurrent.Executor`
* `scala.concurrent.ExecutionContext`

`Executor` 可用于 Java 的 `Future`，而 `ExecutionContext` 可用于 Scala 的 `Future`，因此 Dispatcher 既可用于 Java，又可用于 Scala。

要将 Dispatcher 用于 `Future`，首先要找到它，有两种方式：

* `ActorSystem.dispatcher`
  + actor system 中的 **默认** dispatcher
* `ActorSystem.dispatchers.lookup("xxx")`
  + 查找配置文件中定义的 dispatcher

## Executor

Dispatcher 底层是基于 Executor 的，有两种常见的 `Executor`：

* `ThreadPoolExecutor`
  + 使用队列保存任务，线程从队列中获取、执行任务
* `ForkJoinPool`
  + 使用分治算法 **分解** 任务，然后将分解后的任务交给线程执行
  + work-stealing 算法

`ForkJoinPool` 一般比 `ThreadPoolExecutor` 更加高效！

## 创建 Dispatcher

创建 Dispatcher 就是在 `application.conf` 文件中配置 Dispatcher：

```Scala
my-dispatcher {
  type=Dispatcher
  executor = "fork-join-executor"

  fork-join-executor {
    parallelism-min = 2      # Minimum threads
    parallelism-factor = 2.0 # Maximum threads per core
    parallelism-max = 10     # Maximum total threads
}
  throughput = 100           # Max messages to process in an actor before moving on.
￼}
```

配置 Dispatcher 最基本的是配置 Dispatcher type 和底层使用的 executor 类型，有 4 中 dispatcher 类型可供选择：

* `Dispatcher`
  + 默认类型
  + 大多数场景的最佳选择
* `PinnedDispatcher`
* `CallingThreadDispatcher`
* `BalancingDispatcher`
  + 不推荐直接使用

创建 `Actor` 时，可以不使用默认 Dispatcher，而用 `application.conf` 中配置的 Dispatcher：

```Scala
system.actorOf(Props[MyActor].withDispatcher("my-dispatcher"))
```

## 如何选用合适的 Dispatcher ？

若系统只有一个默认 Dispatcher，则可能出现：

1. 耗时任务 **长时间** 占用线程；
2. 其他请求即使 **可以很快完成**，也必须等待耗时任务结束才能获取 **线程资源**；

可以使用 **多个 Dispatcher**，将耗时任务与其他任务 **隔离**：

* 耗时任务、阻塞任务放在单独的 Dispatcher 中执行；
* 重要任务（例如，响应用户）放在另一个 Dispatcher 中执行；

采用这种方式，首先要识别应用中耗费资源，或可能阻塞的任务。

对于 db client 任务而言：

* 文章解析：CPU 密集型任务
* 使用 JDBC 从 MySQL 读取用户信息：耗时、阻塞
* 获取文章：简单

其中最忌讳的是 **阻塞** 操作，阻塞 IO 的操作千万不能放在 **默认 Dispatcher** 中，以防占用所有线程，阻塞整个应用。

最后，为以上 3 种操作各自分配一个独立的 Dispatcher，将他们 **隔离** 开。

## 默认 Dispatcher

可以用很多方式使用默认 Dispatcher，例如：

* 将 **所有工作** 分离到其他 Dispatcher，默认 Dispatcher 仅执行 Actor 本身的任务；
* 将 **所有高风险工作** 分离到其他 Dispatcher，默认 Dispatcher 仅执行 **异步操作**；

>唯一原则：坚决不能 **阻塞** 默认 Dispatcher 中的线程。

使用默认 Dispatcher 不需要做任务额外工作，若要改变其默认配置，在 `application.conf` 中覆盖默认配置即可：

```Scala
akka {
  actor {
    default-dispatcher {
      throughput = 1
    }
  }
}
```

默认情况下，`Actor` 所有任务都在默认 Dispatcher 中执行。

要使用默认 Dispatcher 执行 `Future`，若在 `Actor` 外部，需要：

```Scala
val system = ActorSystem()
implicit val ec = system.dispatcher

// 使用默认 Dispatcher 执行 Future
val future = Future(() =>println("run in ec"))
```

若在 `Actor` 内部，则直接用即可，因为混入 `Actor` 的类本身就有一个 `implicit dispatcher`：

```Scala
val future = Future(() =>println("run in ec"))
```

**注意**

* 在 `Actor` 内部使用 `Future` 的机会并不多，因为 tell 优先于 ask！

## 阻塞 IO 使用的 Dispatcher

阻塞 IO 的操作应该在单独的 Dispatcher 中执行，假设有如下访问 db 的代码：

```Scala
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface UserProfileRepository extends CrudRepository<UserProfile, Long> {
  List<UserProfile> findById(Long id);
}
```

`findById` 将阻塞当前线程，直到数据库返回值，这时一个 **阻塞操作**，若将其放在默认 Dispatcher 中执行：

```Scala
sender() ! userProfileRepository.findById(id)
```

若有多个 `findById` 调用，则可能 **所有线程** 都被阻塞，因此将 `findById` 放到独立的 Dispatcher 中执行。

第一步，在 `application.conf` 中配置 Dispatcher：

```Scala
blocking-io-dispatcher {
  type = Dispatcher
  executor = "fork-join-executor"
  fork-join-executor {
    parallelism-factor = 50.0
    parallelism-min = 10
    parallelism-max = 100
￼  }
}
```
* `parallelism-factor` 每个 CPU 核心上最多 50 个线程
* `parallelism-min` Dispatcher 最少包含 10 个线程
* `parallelism-max` Dispatcher 最多包含 100 个线程

第二步，查找配置好的 Dispatcher：

```Scala
val ec: ExecutionContext = context.system.dispatchers.lookup("blocking-io-dispatcher")
```

第三步，使用 Dispatcher：

```Scala
val future: Future[UserProfile] = Future{
    userProfileRepository.findById(id)
}(ec)
```

* 也可以将 `ExecutionContext` 声明为 `implicit val`，以免去手动把 `ec` 传递给 `Future` 的麻烦；

## CPU 密集型操作 Dispatcher

* JDBC 例子中，仅将 `Future` 的计算交由独立 Dispatcher 执行；
* 本例中，将 `Actor` 所有任务交由独立 Dispatcher 执行；

第二个目标有两种实现方式：

* 定义 Dispatcher，将其用于 Actor Pool；
* 使用 `BalancingPool` router，其内部使用 `BalancingDispatcher` 实现；

### 1. 为 `Actor` 配置单独的 Dispatcher

第一步，在 `application.conf` 中配置 Dispatcher：

```Scala
article-parsing-dispatcher {
  type = Dispatcher
  executor = "fork-join-executor"
  
  fork-join-executor {
    parallelism-min = 2
    parallelism-factor = 2.0
    parallelism-max = 8
   }

  throughput = 50
}
```

第二步，使用配置好的 Dispatcher 创建 `Actor`：

```Scala
val actors: List[ActorRef] = (0 to 7).map(_ => {
  system.actorOf(Props(classOf[ArticleParseActor]).withDispatche("article-parsing-dispatcher"))
}).toList
```

第三步，使用创建好的 `Actor`，例如用它们创建 router，以便用创建好的 Actor list 进行并行计算：

```Scala
val workerRouter = system.actorOf(
  RoundRobinGroup(actors.map(_.path.toStringWithoutAddress.toList).props(), "workerRouter"))

workRouter.tell(new ParseArticle(TestHelper.file), self());
```

### 2. 使用 `BalancingPool`/`BalancingDispatcher`

`BalancingPool` 是一种 router 类型，底层使用 `BalancingDispatcher` 实现，它有如下特点：

* `BalancingPool` 中所有 `Actor` **共享同一个邮箱**；
* 通过工作窃取机制为空闲 `Actor` 分配任务；

因此，只要邮箱有任务，`BalancingPool` 能保证没有任务空闲 `Actor` 存在，保证大部分 `Actor` 都在工作中，从而提高资源利用率。

第一步，在 `application.conf` 中配置 Dispatcher：

```Scala
pool-dispatcher {
  fork-join-executor { # force it to allocate exactly 8 threads
    parallelism-min = 8
    parallelism-max = 8
  }
  }
```

* Dispatcher 中，正好有 8 个线程；

第二步，创建 `BalancingPool` Router：

```Scala
val workerRouter = system.actorOf(BalancingPool(8).props(Props(classOf[ArticleParseActor])),
 "balancing-pool-router")
```

* pool 中的 `Actor` 数量最好与 Dispatcher 中线程数量一致！

>`BalancingPool` 是为 **本地 `Actor` 资源** 均衡负载的绝佳方式！
