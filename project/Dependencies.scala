import sbt._

object Versions {
  val Circe = "0.13.0"
  val Logback = "1.2.3"
  val PureConfig = "0.12.2"
  val Zio = "1.0.1"
  val Http4s = "1.0.0-M3"
}

object Dependencies {

  lazy val zio = Seq(
    "dev.zio" %% "zio" % Versions.Zio,
    "dev.zio" %% "zio-macros" % Versions.Zio,
    "dev.zio" %% "zio-interop-cats" % "2.1.4.0",
    "dev.zio" %% "zio-test" % Versions.Zio % Test,
    "dev.zio" %% "zio-test-sbt" % Versions.Zio % Test
  )

  lazy val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-generic-extras",
    "io.circe" %% "circe-parser"
  ).map(_ % Versions.Circe)

  lazy val http4s = Seq(
    "org.http4s" %% "http4s-blaze-server",
    "org.http4s" %% "http4s-circe",
    "org.http4s" %% "http4s-dsl",
    "org.http4s" %% "http4s-blaze-client"
  ).map(_ % Versions.Http4s)

  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % Versions.PureConfig
  lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.Logback
}
