package jupposessho.service

import jupposessho.config.Configuration.ServerConfig
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Bootstrap {

  def server(config: ServerConfig, routes: HttpApp[Task]) =
    ZIO
      .runtime[ZEnv]
      .flatMap { implicit rts =>
        BlazeServerBuilder[Task](rts.platform.executor.asEC)
          .bindHttp(config.port, config.host)
          .withHttpApp(routes)
          .serve
          .compile
          .drain
      }
}
