package jupposessho.service

import com.danielasfregola.twitter4s.exceptions.TwitterException
import jupposessho.client.TwitterClient
import jupposessho.model.{AppError, User}
import jupposessho.model.AppError._
import zio._
import zio.logging.Logger

object TwitterService {

  trait Service {
    def friends(source: User, target: User): IO[List[AppError], Boolean]
  }

  def apply(client: TwitterClient.Service, log: Logger[String]) = new Service {

    val logger = log.named("TwitterService")

    def friends(source: User, target: User): IO[List[AppError], Boolean] = {
      client
        .relationshipBetweenUsers(source, target)
        .tapBoth(error => log.error(s"twitter relationship error: $error"),
                 relationshipResult => log.info(s"twitter relationship: $relationshipResult"))
        .map { rated =>
          val relationship = rated.data.relationship
          relationship.source.followed_by && relationship.source.following && relationship.target.followed_by && relationship.target.following
        }
        .refineToOrDie[TwitterException]
        .mapError { t =>
          t.errors.errors.toList.map { error =>
            if (error.code == 163) {
              TwitterUserNotFound(source)
            } else if (error.code == 50) {
              TwitterUserNotFound(target)
            } else {
              TwitterError(t.getMessage())
            }
          }
        }
    }
  }
}
