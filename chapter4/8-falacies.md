# 分布式计算的 8 大误区

Sun 公司总结了 8 个初级工程师常犯的错误。

## 1. The network is reliable

这是最常见的误解，我们很容易用处理 **本地系统** 的方式来处理 **远程系统**，Akka 的位置透明性也容易误导大家认为网络是可靠的。

实际上，网络故障是非常常见的，AWS 每天都会出现连接失败、网络错误等。

## 2. Latency is zero

延迟来源多种多样：

* 消息在网络上传输
* 响应在网络上返回
* 远程 `Actor` 比 本地 `Actor` 延迟更多

建立连接之前：

1. hostname => ip(DNS)
2. ip => MAC address(ARP)
3. 握手，建立 TCP 连接
4. 通过 TCP 连接发送数据

现在可以看到，网络请求的创建开销很大，因此需要 **减少** 请求数量，**增大** 每个请求的大小，具体到本书，最能能到 **单个消息** 中发送多个 `SetRequest` 或 `GetRequest`。

## 3. Bandwidth is infinite

为减少带宽消耗，可以消息进行压缩，即：

* 将序列化后的消息发送到 remote `Actor` 之前，对压缩；
* 接受到消息后，先解压，然后反序列化；

## 4. The network is secure

## 5. Network topology doesn't change

网络拓扑在不断变化，这点在云环境更加明显：每次重启可能 IP 就不同了。

可以用 zookeeper 等提供服务注解避免网络拓扑带来的问题。

## 6. There is one administrator

减少管理入口，减少配置修改入口。

## 7. Transport cost is zero

## 8. The network is homogeneous

比如不同部署环境中，防火墙、负载均衡器等等，都可能不同，应用不应该对它们有所假设。