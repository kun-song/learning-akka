name := "akkademy-client"

version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies ++= Seq(
  "org.scalactic"     %% "scalactic" % "3.0.5",
  "org.scalatest"     %% "scalatest" % "3.0.5" % "test",

  "com.akkademy-db"   %% "akkademy-db" % "0.1"
)
