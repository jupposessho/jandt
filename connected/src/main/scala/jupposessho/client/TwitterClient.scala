package jupposessho.client

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{RatedData, Relationship}
import zio._

object TwitterClient {

  trait Service {
    def relationshipBetweenUsers(source: String, target: String): Task[RatedData[Relationship]]
  }

  def apply(restClient: TwitterRestClient) = new Service {
    def relationshipBetweenUsers(source: String, target: String): Task[RatedData[Relationship]] = {
      ZIO.fromFuture(_ => restClient.relationshipBetweenUsers(source, target))
    }
  }
}
