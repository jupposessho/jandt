package jupposessho.config

import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio._

object Configuration {

  type Configuration = Has[ServerConfig]

  final case class ServerConfig(host: String, port: Int)
  final case class AppConfig(server: ServerConfig)

  def load() =
    Task
      .effect(ConfigSource.default.loadOrThrow[AppConfig])
      .toManaged_
}
