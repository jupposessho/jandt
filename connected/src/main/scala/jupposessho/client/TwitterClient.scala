package jupposessho.client

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{RatedData, Relationship}
import jupposessho.model.User
import zio._

object TwitterClient {

  trait Service {
    def relationshipBetweenUsers(source: User, target: User): Task[RatedData[Relationship]]
  }

  def apply(restClient: TwitterRestClient) = new Service {
    def relationshipBetweenUsers(source: User, target: User): Task[RatedData[Relationship]] = {
      ZIO.fromFuture(_ => restClient.relationshipBetweenUsers(source.name, target.name))
    }
  }
}
