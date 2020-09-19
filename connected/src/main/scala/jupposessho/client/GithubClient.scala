package jupposessho.client

import jupposessho.config.Configuration.GithubConfig
import jupposessho.model.User
import jupposessho.model.github.Organization
import org.http4s.client._
import org.http4s.Uri
import zio._
import zio.clock.Clock

object GithubClient {

  trait Service {
    def organizations(user: User): Task[List[Organization]]
  }

  def apply(restClient: Client[Task], clock: Clock.Service, config: GithubConfig) = new Service {

    def organizations(user: User): Task[List[Organization]] = {
      val targetUrl = s"${config.url}/users/${user.name}/orgs"
      Task
        .fromEither(Uri.fromString(targetUrl))
        .flatMap(uri => restClient.expect[List[Organization]](uri.toString()))
        .retry(Schedule.fibonacci(config.duration) && Schedule.recurs(config.retryCount))
        .provideLayer(ZLayer.succeed(clock))
    }
  }
}
