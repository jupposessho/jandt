package jupposessho

import jupposessho.config.Configuration
import jupposessho.service.Bootstrap
import zio._
import zio.stream.ZStream

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {

    def program =
      for {
        config <- Configuration.load()
        restClient <- Bootstrap.client().toManaged_.flatten
        routes = Bootstrap.routes(restClient, config)
        _ <- ZStream
          .mergeAllUnbounded()(ZStream.fromEffect(Bootstrap.server(config.server, routes)))
          .runDrain
          .toManaged_
      } yield ()

    program.use_(ZIO.unit).exitCode
  }
}
