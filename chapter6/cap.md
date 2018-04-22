# CAP

CAP 是分布式系统 3 个特性的缩写：

* consistency 一致性
  + 所有节点访问同一份 **最新** 的数据副本
* availability 可用性
  + 未失败节点能合理响应
  + 失败节点不会影响未失败节点的响应
* partition tolerance 分区容忍性
  + 分区指 **网络分区**，分布式系统中，各个节点应该是网络连通的，但可能因为网络故障，某些节点不再连通，整个网络就分为了几个区域，这就是分区
  + 若数据只在存放在 **一个** 节点中，若出现分区，则与该结点不连通的区域就无法获取其数据，此时分区就是无法容忍的
  + 若数据存在于 **多个** 结点中，即使出现分区，该数据可以从其他节点获取，从而 **提升** 了分区容忍性（但会影响一致性）

CAP 理论已证明，分布式系统只能选择 3 者其 2，无法同时满足 3 者，当然，这时非常简化的说法，实际上 3 者并非完全无法满足，而是 3 者间需要有 **偏向**，无法同时 **完美满足** 3 者。

## 首要选择

一般都希望分布式系统具备 partition tolerance，而 consistency 和 availablity 之间侧重满足其一。

* 分布式系统不满足 partition tolerance 就没有意义了，直接做单机好了。

## 1. CP 系统：强一致性

最简单的强一致性数据库有：

* 一个 master 节点
* 多个 slave 节点

为保证一致性，读、写操作必须走 master 节点。

若 master 节点失败，则需要选举出新的 master，选举期间无法写入、读取，因此我们放弃了 availablity，选择了 consistency。

例子：

* Redis Sentinel
* 冗余备份的 RDBMS

## 2. AP 系统：最终一致性

选择 availability 和 partition tolerance 的系统具备 **高可用** + **最终一致性**。

AP 系统可以有多种实现，例如，假设有 3 个节点，每个结点有：

* 1 个 master
* 2 个 slave

读写：

* 每个节点都支持写入，然后自动备份到其他 2 个节点；
* 每个节点都支持读取；

但读取时，数据同步可能未完成，读取的数据可能 **不是最新的**，因此不具备 **一致性**，但等同步完成后，数据就是最新的了，因此具备 **最终一致性**。

### 不同程度的 consistency

#### AP 系统

AP 系统的例子中，read 可以提供不同程度的一致性：

* 从 1 个节点读：最弱 consistency，最强 partition tolerance
* 从 2 个节点读：增强 consistency，减弱 partition tolerance
* 从 3 个节点读：最强 consistency，最弱 partition tolerance

实际系统中，一般选择 **大多数节点** 可用，并返回一致的结果即可，从而在 consistency 和 partition tolerance 之间做出权衡。

#### CP 系统

CP 系统例子中，为获取 **强一致性**，writer read 都走 master 节点，但也可以这样设计：

* write 走 master
* read 走 master + slave

这样 writer 依然具备强一致性，但 read 具备最终一致性，从而在 read 操作上，数据库变成了最终一致性系统。

>**总结**
>
>consistency 和 availability 需要根据实际场景做出设计取舍。
