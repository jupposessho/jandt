import Dependencies._
import sbt.Keys.{scalacOptions, _}

lazy val commonSettings = Seq(
  organization := "jupposessho",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.13.2",
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  cancelable in Global := true,
  fork in Global := true
)

lazy val connected = project
  .in(file("connected"))
  .enablePlugins(JavaAppPackaging, PackPlugin)
  .settings(commonSettings)
  .settings(
    name := "connected",
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= zio ++ http4s ++ circe :+ pureConfig :+ logback :+ twitter
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "jandt",
    skip in publish := true,
    Global / onChangedBuildSource := ReloadOnSourceChanges
  )
  .aggregate(connected)
