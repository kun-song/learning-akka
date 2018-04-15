name := "akkademy-db"
organization := "com.akkademy-db"
version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.12",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.12" % Test,
  "com.typesafe.akka" %% "akka-remote" % "2.5.12",

  "org.scalactic"     %% "scalactic" % "3.0.5",
  "org.scalatest"     %% "scalatest" % "3.0.5" % "test",

  "junit"             % "junit" % "4.12" % "test",
  "com.novocode"      % "junit-interface" % "0.11" % "test"
)

/**
  * 防止发布 application.conf
  */
mappings in (Compile, packageBin) ~= { _.filterNot {
  case (_, name)  ⇒ Seq("application.conf").contains(name)
}}