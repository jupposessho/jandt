package jupposessho.service

import com.danielasfregola.twitter4s.TwitterRestClient
import jupposessho.config.Configuration.{AppConfig, ServerConfig}
import jupposessho.client.{GithubClient, TwitterClient}
import jupposessho.routes.ConnectedRoutes
import jupposessho.service.{ConnectedService, GithubService, TwitterService}
import org.http4s.HttpApp
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext.Implicits
import zio._
import zio.clock.Clock
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.logging.Logger

object Bootstrap {

  def server(config: ServerConfig, routes: HttpApp[Task]) =
    ZIO
      .runtime[ZEnv]
      .flatMap { implicit rts =>
        BlazeServerBuilder[Task](rts.platform.executor.asEC)
          .bindHttp(config.port, config.host)
          .withHttpApp(routes)
          .serve
          .compile
          .drain
      }

  def client() =
    ZIO
      .runtime[Any]
      .map { implicit rts =>
        BlazeClientBuilder
          .apply[Task](Implicits.global)
          .resource
          .toManaged
      }

  def routes(restClient: Client[Task], config: AppConfig, log: Logger[String]) = {
    val clock = Clock.Service.live
    val githubClient = GithubClient(restClient, clock, config.github)
    val githubService = GithubService(githubClient, log)
    val twitterRestClient = TwitterRestClient(config.twitter.consumer, config.twitter.access)
    val twitterClient = TwitterClient(twitterRestClient, Clock.Service.live, config.twitter)
    val twitterService = TwitterService(twitterClient, log)
    val connectedService = ConnectedService(githubService, twitterService, log)

    ConnectedRoutes(connectedService).routes()
  }
}
