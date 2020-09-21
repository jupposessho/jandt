package jupposessho.client

import jupposessho.config.Configuration.GithubConfig
import jupposessho.model.User
import jupposessho.model.github.Organization
import org.http4s.client._
import org.http4s.Uri
import zio._
import zio.clock.Clock
import zio.logging.Logger

object GithubClient {

  trait Service {
    def organizations(user: User): Task[List[Organization]]
  }

  def apply(restClient: Client[Task],
            clock: Clock.Service,
            config: GithubConfig,
            log: Logger[String],
            cache: Ref[Map[String, List[Organization]]]) = new Service {

    val logger = log.named("GithubClient")

    def organizations(user: User): Task[List[Organization]] = {
      for {
        storedResults <- cache.get
        result <- storedResults.get(user.name) match {
          case Some(value) =>
            logger.info(s"serving github organizations from cache for user: ${user.name}") *>
              Task.succeed(value)
          case None =>
            val targetUrl = s"${config.url}/users/${user.name}/orgs"
            Task
              .fromEither(Uri.fromString(targetUrl))
              .flatMap(uri => restClient.expect[List[Organization]](uri.toString()))
              .retry(Schedule.fibonacci(config.duration) && Schedule.recurs(config.retryCount))
              .tap { orgs =>
                cache.update(_ + (user.name -> orgs)) *>
                  logger.info(s"caching github organizations for user: ${user.name}")
              }
              .provideLayer(ZLayer.succeed(clock))
        }
      } yield result
    }
  }
}
