# 示例问题

创建一个新的客户端 `akkademaid`，接受文章 URL，返回文章的文本内容，并将缓存已解析的文章内容。

依赖：

```Scala
libraryDependencies ++= Seq(
  "com.akkademy-db"   %% "akkademy-db" % "0.1",
  "com.syncthemall"   % "boilerpipe"   % "1.2.2"
)
```

`boilerpipe` 提供了文章解析的功能：`ArticleExtractor.getInstance.getText(input)`。