package jupposessho

import jupposessho.config.Configuration
import jupposessho.routes.ConnectedRoutes
import jupposessho.service.Bootstrap._
import zio._
import zio.stream.ZStream

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {

    def program =
      for {
        config <- Configuration.load()
        routes = ConnectedRoutes().routes()
        _ <- ZStream
          .mergeAllUnbounded()(ZStream.fromEffect(server(config.server, routes)))
          .runDrain
          .toManaged_
      } yield ()

    program.use_(ZIO.unit).exitCode
  }
}
