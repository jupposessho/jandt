package jupposessho.service

import com.danielasfregola.twitter4s.exceptions.TwitterException
import jupposessho.client.TwitterClient
import jupposessho.model.AppError
import jupposessho.model.AppError._
import zio._

object TwitterService {

  trait Service {
    def friends(source: String, target: String): IO[List[AppError], Boolean] // TODO value class
  }

  def apply(client: TwitterClient.Service) = new Service {
    def friends(source: String, target: String): IO[List[AppError], Boolean] = {
      client
        .relationshipBetweenUsers(source, target)
        .map { rated =>
          val relationship = rated.data.relationship
          relationship.source.followed_by && relationship.source.following && relationship.target.followed_by && relationship.target.following
        }
        .refineToOrDie[TwitterException]
        .mapError { t =>
          t.errors.errors.toList.map { error =>
            if (error.code == 163) {
              UserNotFound(source)
            } else if (error.code == 50) {
              UserNotFound(target)
            } else {
              TwitterError(t.getMessage())
            }
          }
        }
    }
  }
}
