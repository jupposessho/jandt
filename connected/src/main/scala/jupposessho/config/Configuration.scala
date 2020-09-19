package jupposessho.config

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto._
import scala.concurrent.duration.FiniteDuration
import zio._
import zio.duration.Duration

object Configuration {

  type Configuration = Has[ServerConfig]

  implicit val zioDurationReader = ConfigReader[FiniteDuration].map(Duration.fromScala)

  final case class GithubConfig(url: String, retryCount: Int, duration: Duration)
  final case class ServerConfig(host: String, port: Int)

  final case class AppConfig(server: ServerConfig, github: GithubConfig)

  def load() =
    Task
      .effect(ConfigSource.default.loadOrThrow[AppConfig])
      .toManaged_
}
