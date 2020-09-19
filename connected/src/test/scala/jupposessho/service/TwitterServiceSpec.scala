package jupposessho.service

import akka.http.scaladsl.model.StatusCodes
import jupposessho.client.TwitterClient
import jupposessho.model.{AppError, User}
import com.danielasfregola.twitter4s.entities._
import com.danielasfregola.twitter4s.exceptions.{Errors, TwitterError, TwitterException}
import zio._
import zio.test._
import zio.test.Assertion._
import java.time.Instant

object TwitterServiceSpec extends DefaultRunnableSpec {

  val sourceUser = User("source")
  val targetUser = User("target")

  override def spec = suite("TwitterService.friends should")(
    suite("return true")(testM("when the given accounts are friends") {
      val client = TestTwitterClient(true, true, true, true)
      assertM(TwitterService(client).friends(sourceUser, targetUser))(equalTo(true))
    }),
    suite("return false")(
      testM("when source is not followed") {
        val client = TestTwitterClient(false, true, true, true)
        assertM(TwitterService(client).friends(sourceUser, targetUser))(equalTo(false))
      },
      testM("when target is not followed") {
        val client = TestTwitterClient(true, true, false, true)
        assertM(TwitterService(client).friends(sourceUser, targetUser))(equalTo(false))
      },
      testM("when target is not following") {
        val client = TestTwitterClient(true, true, true, false)
        assertM(TwitterService(client).friends(sourceUser, targetUser))(equalTo(false))
      },
      testM("when source is not following") {
        val client = TestTwitterClient(true, false, true, true)
        assertM(TwitterService(client).friends(sourceUser, targetUser))(equalTo(false))
      }
    ),
    suite("fail")(
      testM("when source does not exist") {
        val client = FailingTwitterClient(
          TwitterException(StatusCodes.NotFound, Errors(TwitterError("Could not determine source user.", 163)))
        )
        assertM(TwitterService(client).friends(sourceUser, targetUser).flip)(
          equalTo(List(AppError.TwitterUserNotFound(sourceUser)))
        )
      },
      testM("when target does not exist") {
        val client = FailingTwitterClient(
          TwitterException(StatusCodes.NotFound, Errors(TwitterError("User not found.", 50)))
        )
        assertM(TwitterService(client).friends(sourceUser, targetUser).flip)(
          equalTo(List(AppError.TwitterUserNotFound(targetUser)))
        )
      },
      testM("when other twitter error occurred") {
        val client = FailingTwitterClient(
          TwitterException(StatusCodes.InternalServerError, Errors(TwitterError("Something", 0)))
        )
        assertM(TwitterService(client).friends(sourceUser, targetUser).flip)(
          equalTo(List(AppError.TwitterError("[500 Internal Server Error] Something (0)")))
        )
      }
    )
  )

  object TestTwitterClient {
    def apply(sourceFollowed: Boolean, sourceFollowing: Boolean, targetFollowed: Boolean, targetFollowing: Boolean) =
      new TwitterClient.Service {
        def relationshipBetweenUsers(source: User, target: User): Task[RatedData[Relationship]] =
          Task.succeed(
            RatedData(
              RateLimit(1, 0, Instant.now()),
              Relationship(
                RelationshipOverview(
                  RelationshipSource(1, "ids", "ids", followed_by = sourceFollowed, following = sourceFollowing),
                  RelationshipTarget(2, "idt", "idt", followed_by = targetFollowed, following = targetFollowing)
                )
              )
            )
          )
      }
  }

  object FailingTwitterClient {
    def apply(exception: Throwable) =
      new TwitterClient.Service {
        def relationshipBetweenUsers(source: User, target: User): Task[RatedData[Relationship]] =
          IO.fail(exception)
      }
  }
}
