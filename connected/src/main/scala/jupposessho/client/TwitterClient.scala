package jupposessho.client

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{RatedData, Relationship}
import jupposessho.config.Configuration.TwitterConfig
import jupposessho.model.User
import zio._
import zio.clock.Clock
import zio.logging.Logger

object TwitterClient {

  type Cache = Map[(String, String), RatedData[Relationship]]
  val emptyCache = Map.empty[(String, String), RatedData[Relationship]]
  trait Service {
    def relationshipBetweenUsers(source: User, target: User): Task[RatedData[Relationship]]
  }

  def apply(restClient: TwitterRestClient,
            clock: Clock.Service,
            config: TwitterConfig,
            log: Logger[String],
            cache: Ref[Cache]) =
    new Service {

      val logger = log.named("TwitterClient")

      def relationshipBetweenUsers(source: User, target: User): Task[RatedData[Relationship]] = {
        val cacheKey =
          if ((source.name < target.name)) Tuple2(source.name, target.name) else Tuple2(target.name, source.name)

        for {
          storedResults <- cache.get
          result <- storedResults.get(cacheKey) match {
            case Some(value) =>
              logger.info(s"serving twitter relationship from cache for users: ${source.name} and ${target.name}") *>
                Task.succeed(value)
            case None =>
              ZIO
                .fromFuture(_ => restClient.relationshipBetweenUsers(source.name, target.name))
                .retry(Schedule.fibonacci(config.duration) && Schedule.recurs(config.retryCount))
                .tap { relations =>
                  cache.update(_ + (cacheKey -> relations)) *>
                    logger.info(s"caching twitter relationship for uses: ${source.name} and ${target.name}")
                }
                .provideLayer(ZLayer.succeed(clock))
          }
        } yield result
      }
    }
}
