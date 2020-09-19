package jupposessho.client

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{RatedData, Relationship}
import jupposessho.config.Configuration.TwitterConfig
import jupposessho.model.User
import zio._
import zio.clock.Clock

object TwitterClient {

  trait Service {
    def relationshipBetweenUsers(source: User, target: User): Task[RatedData[Relationship]]
  }

  def apply(restClient: TwitterRestClient, clock: Clock.Service, config: TwitterConfig) = new Service {
    def relationshipBetweenUsers(source: User, target: User): Task[RatedData[Relationship]] = {
      ZIO
        .fromFuture(_ => restClient.relationshipBetweenUsers(source.name, target.name))
        .retry(Schedule.fibonacci(config.duration) && Schedule.recurs(config.retryCount))
        .provideLayer(ZLayer.succeed(clock))
    }
  }
}
