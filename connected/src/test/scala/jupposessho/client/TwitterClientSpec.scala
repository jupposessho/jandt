package jupposessho.client

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities._
import java.time.Instant
import jupposessho.TestDependencies
import jupposessho.config.Configuration.TwitterConfig
import jupposessho.model.User
import scala.concurrent.Future
import zio._
import zio.clock.Clock
import zio.duration._
import zio.logging.Logger
import zio.test._
import zio.test.Assertion._

object TwitterClientSpec extends DefaultRunnableSpec {

  val config = TwitterConfig(ConsumerToken("", ""), AccessToken("", ""), 1, 1.second)
  val relation = RatedData(
    RateLimit(1, 0, Instant.now()),
    Relationship(
      RelationshipOverview(
        RelationshipSource(1, "ids", "ids", followed_by = true, following = true),
        RelationshipTarget(2, "idt", "idt", followed_by = true, following = true)
      )
    )
  )

  override def spec =
    suite("TwitterClientSpec.relationshipBetweenUsers should")(
      suite("return the relationship of the given users")(
        testM("when rest call succeed") {
          for {
            clientCalls <- Ref.make(List.empty[String])
            client = new TestRestClient(clientCalls, relation, config.consumer, config.access)
            cache <- Ref.make(TwitterClient.emptyCache)
            log <- ZIO.environment[Has[Logger[String]]].map(_.get)
            result <- TwitterClient(client, Clock.Service.live, config, log, cache)
              .relationshipBetweenUsers(User("source"), User("target"))
          } yield assert(result)(equalTo(relation))
        },
        testM("when second call cached") {
          for {
            clientCalls <- Ref.make(List.empty[String])
            client = new TestRestClient(clientCalls, relation, config.consumer, config.access)
            cache <- Ref.make(TwitterClient.emptyCache)
            log <- ZIO.environment[Has[Logger[String]]].map(_.get)
            twitterClient = TwitterClient(client, Clock.Service.live, config, log, cache)
            _ <- twitterClient.relationshipBetweenUsers(User("source"), User("target"))
            _ <- twitterClient.relationshipBetweenUsers(User("source"), User("target"))
            _ <- twitterClient.relationshipBetweenUsers(User("target"), User("source"))
            _ <- twitterClient.relationshipBetweenUsers(User("source2"), User("target2"))
            restCalls <- clientCalls.get
          } yield assert(restCalls)(equalTo(List("source2|target2", "source|target")))
        }
      )
    ).provideCustomLayer(TestDependencies.logger("TwitterClientSpec"))

  class TestRestClient(calls: Ref[List[String]],
                       relation: RatedData[Relationship],
                       consumerToken: ConsumerToken,
                       accessToken: AccessToken)
      extends TwitterRestClient(consumerToken, accessToken) {
    override def relationshipBetweenUsers(source_screen_name: String,
                                          target_screen_name: String): Future[RatedData[Relationship]] =
      Runtime.default.unsafeRunToFuture(
        calls.update(s"$source_screen_name|$target_screen_name" :: _) *> Task.succeed(relation)
      )
  }
}
