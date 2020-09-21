package jupposessho.client

import cats.data.Kleisli
import cats.effect.Resource
import jupposessho.TestDependencies
import jupposessho.config.Configuration.GithubConfig
import jupposessho.model.User
import jupposessho.model.github.Organization
import org.http4s.{EntityDecoder, Request, Response, Status, Uri}
import org.http4s.client._
import zio._
import zio.clock.Clock
import zio.duration._
import zio.logging.Logger
import zio.test._
import zio.test.Assertion._

object GithubClientSpec extends DefaultRunnableSpec {

  val user = User("user")
  val organizations = List(Organization(3, "name", "url"))

  override def spec =
    suite("GithubClient.organizations should")(
      suite("return the organizations of the given user")(
        testM("when rest call succeed") {
          for {
            clientCalls <- Ref.make(List.empty[String])
            client = TestRestClient(clientCalls, organizations)
            cache <- Ref.make(Map.empty[String, List[Organization]])
            log <- ZIO.environment[Has[Logger[String]]].map(_.get)
            result <- GithubClient(client, Clock.Service.live, GithubConfig("", 1, 1.second), log, cache)
              .organizations(user)
          } yield assert(result)(equalTo(organizations))
        },
        testM("when second call cached") {
          for {
            clientCalls <- Ref.make(List.empty[String])
            client = TestRestClient(clientCalls, organizations)
            cache <- Ref.make(Map.empty[String, List[Organization]])
            log <- ZIO.environment[Has[Logger[String]]].map(_.get)
            githubClient = GithubClient(client, Clock.Service.live, GithubConfig("", 1, 1.second), log, cache)
            _ <- githubClient.organizations(user)
            _ <- githubClient.organizations(User("other_user"))
            _ <- githubClient.organizations(user)
            restCalls <- clientCalls.get
            cacheState <- cache.get
          } yield (
            assert(restCalls)(equalTo(List("/users/other_user/orgs", "/users/user/orgs"))) &&
              assert(cacheState)(equalTo(Map("user" -> organizations, "other_user" -> organizations)))
          )
        }
      )
    ).provideCustomLayer(TestDependencies.logger("GithubClientSpec"))

  object TestRestClient {
    def apply(calls: Ref[List[String]], response: List[Organization]) =
      new Client[Task] {

        override def expect[A](s: String)(implicit d: EntityDecoder[zio.Task, A]): zio.Task[A] =
          calls.update(s :: _) *> zio.Task.succeed(response.asInstanceOf[A])

        override def run(req: Request[zio.Task]): Resource[zio.Task, Response[zio.Task]] = ???

        override def fetch[A](req: Request[zio.Task])(f: Response[zio.Task] => zio.Task[A]): zio.Task[A] = ???

        override def fetch[A](req: zio.Task[Request[zio.Task]])(f: Response[zio.Task] => zio.Task[A]): zio.Task[A] = ???

        override def toKleisli[A](f: Response[zio.Task] => zio.Task[A]): Kleisli[zio.Task, Request[zio.Task], A] = ???

        override def toHttpApp: org.http4s.HttpApp[zio.Task] = ???

        override def stream(req: Request[zio.Task]): fs2.Stream[zio.Task, Response[zio.Task]] = ???

        override def streaming[A](req: Request[zio.Task])(
          f: Response[zio.Task] => fs2.Stream[zio.Task, A]
        ): fs2.Stream[zio.Task, A] = ???

        override def streaming[A](req: zio.Task[Request[zio.Task]])(
          f: Response[zio.Task] => fs2.Stream[zio.Task, A]
        ): fs2.Stream[zio.Task, A] = ???

        override def expectOr[A](req: Request[zio.Task])(onError: Response[zio.Task] => zio.Task[Throwable])(
          implicit d: EntityDecoder[zio.Task, A]
        ): zio.Task[A] = ???

        override def expect[A](req: Request[zio.Task])(implicit d: EntityDecoder[zio.Task, A]): zio.Task[A] = ???

        override def expectOr[A](req: zio.Task[Request[zio.Task]])(onError: Response[zio.Task] => zio.Task[Throwable])(
          implicit d: EntityDecoder[zio.Task, A]
        ): zio.Task[A] = ???

        override def expect[A](req: zio.Task[Request[zio.Task]])(implicit d: EntityDecoder[zio.Task, A]): zio.Task[A] =
          ???

        override def expectOr[A](uri: Uri)(onError: Response[zio.Task] => zio.Task[Throwable])(
          implicit d: EntityDecoder[zio.Task, A]
        ): zio.Task[A] = ???

        override def expect[A](uri: Uri)(implicit d: EntityDecoder[zio.Task, A]): zio.Task[A] = ???

        override def expectOr[A](s: String)(onError: Response[zio.Task] => zio.Task[Throwable])(
          implicit d: EntityDecoder[zio.Task, A]
        ): zio.Task[A] = ???

        override def expectOptionOr[A](req: Request[zio.Task])(onError: Response[zio.Task] => zio.Task[Throwable])(
          implicit d: EntityDecoder[zio.Task, A]
        ): zio.Task[Option[A]] = ???

        override def expectOption[A](req: Request[zio.Task])(
          implicit d: EntityDecoder[zio.Task, A]
        ): zio.Task[Option[A]] = ???

        override def fetchAs[A](req: Request[zio.Task])(implicit d: EntityDecoder[zio.Task, A]): zio.Task[A] = ???

        override def fetchAs[A](req: zio.Task[Request[zio.Task]])(implicit d: EntityDecoder[zio.Task, A]): zio.Task[A] =
          ???

        override def status(req: Request[zio.Task]): zio.Task[Status] = ???

        override def status(req: zio.Task[Request[zio.Task]]): zio.Task[Status] = ???

        override def statusFromUri(uri: Uri): zio.Task[Status] = ???

        override def statusFromString(s: String): zio.Task[Status] = ???

        override def successful(req: Request[zio.Task]): zio.Task[Boolean] = ???

        override def successful(req: zio.Task[Request[zio.Task]]): zio.Task[Boolean] = ???

        override def get[A](uri: Uri)(f: Response[zio.Task] => zio.Task[A]): zio.Task[A] = ???

        override def get[A](s: String)(f: Response[zio.Task] => zio.Task[A]): zio.Task[A] = ???

      }
  }
}
