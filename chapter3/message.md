# 消息定义

## 不可变消息

消息最好是不可变的，但 Akka 并 **没有强制** 不可变，**可变引用** or **可变类型** 都会导致可变消息。

### 最差的消息定义

可变引用 + 可变类型：

```Scala
class Message(var buffer: StringBuffer = new StringBuffer())
```

* `buffer` 是可变引用；
* `StringBuffer` 是可变类型；

`var` 导致 `buffer` 是可变应用，可以指向另一个对象：

```Scala
val m = new Message(new StringBuffer("abc"))
m.buffer = new StringBuffer("efg)
```

### 次差的消息定义

不可变引用 + 可变类型：

```Scala
class Message(buffer: StringBuffer = new StringBuffer())
```

* 不加 `var`，默认就是 `val`；

现在无法让 `buffer` 指向另一个对象了，但是依然可以修改 `buffer` 的内容，因此依然不是不可变消息：

```Scala
val m = new Message(new StringBuffer("abc"))
m.append("cfg)
```

### 不可变消息定义

**不可变引用** + **不可变类型**：

```Scala
class Message(buffer: String)
```

* 将 `StringBuffer` 修改为 `String`；

### `case class` 消息

Scala 中推荐使用 `case class` 定义消息：

* `case class` 会自动生成一些有用的方法，例如 `toString` 和 `copy()`；
* `case class` 支持 **序列化**；

```Scala
case class Message(buffer: String)
```
