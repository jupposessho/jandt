package jupposessho

import jupposessho.config.Configuration
import jupposessho.client.TwitterClient
import jupposessho.model.github.Organization
import jupposessho.service.Bootstrap
import zio._
import zio.logging.Logger
import zio.logging.slf4j.Slf4jLogger
import zio.stream.ZStream

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {

    val logger = Slf4jLogger.make(
      logFormat = (_, logEntry) => logEntry,
      rootLoggerName = Some("ConnectedMain")
    )

    def program =
      for {
        config <- Configuration.load()
        log <- ZIO.environment[Has[Logger[String]]].map(_.get).toManaged_
        restClient <- Bootstrap.client().toManaged_.flatten
        githubCache <- Ref.make(Map.empty[String, List[Organization]]).toManaged_
        twitterCache <- Ref.make(TwitterClient.emptyCache).toManaged_
        routes = Bootstrap.routes(restClient, config, log, githubCache, twitterCache)
        _ <- ZStream
          .mergeAllUnbounded()(ZStream.fromEffect(Bootstrap.server(config.server, routes)))
          .runDrain
          .toManaged_
      } yield ()

    program
      .use_(ZIO.unit)
      .provideCustomLayer(logger)
      .exitCode
  }
}
