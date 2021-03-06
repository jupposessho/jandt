package jupposessho.service

import jupposessho.TestDependencies
import jupposessho.model.{AppError, ConnectedResult, User}
import jupposessho.model.github.Organization
import zio._
import zio.logging.Logger
import zio.test._
import zio.test.Assertion._

object ConnectedServiceSpec extends DefaultRunnableSpec {

  val sourceUser = User("source")
  val targetUser = User("target")
  val githubErrors = List(AppError.GithubError("github error"))
  val twitterErrors = List(AppError.TwitterError("twitter error"))

  override def spec =
    suite("ConnectedService.connected should")(
      suite("return ConnectedResult")(
        testM("when the given accounts are connected") {
          val organization = Organization(1, "login", "url")
          assertConnected(List(organization), true, ConnectedResult(true, Some(List(organization.login))))
        },
        testM("when the given accounts are not friends on Twitter") {
          val organization = Organization(1, "login", "url")
          assertConnected(List(organization), false, ConnectedResult(false, None))
        },
        testM("when the given accounts have no common organization on Github") {
          assertConnected(Nil, true, ConnectedResult(false, None))
        }
      ),
      suite("fail")(
        testM("when Twitter service fails") {
          assertFailConnected(IO.succeed(Nil), IO.fail(twitterErrors), twitterErrors)
        },
        testM("when Github service fails") {
          assertFailConnected(IO.fail(githubErrors), IO.succeed(true), githubErrors)
        },
        testM("when both service fail - with aggregated errors") {
          assertFailConnected(IO.fail(githubErrors), IO.fail(twitterErrors), githubErrors ++ twitterErrors)
        }
      )
    ).provideCustomLayer(TestDependencies.logger("ConnectedServiceSpec"))

  private def assertConnected(githubResult: List[Organization],
                              twitterResult: Boolean,
                              expectedResult: ConnectedResult) = {
    for {
      log <- ZIO.environment[Has[Logger[String]]].map(_.get)
      service = ConnectedService(TestGithubService(IO.succeed(githubResult)),
                                 TestTwitterService(IO.succeed(twitterResult)),
                                 log)
      result <- service.connected(sourceUser, targetUser)
    } yield assert(result)(equalTo(expectedResult))
  }

  private def assertFailConnected(githubResult: IO[List[AppError], List[Organization]],
                                  twitterResult: IO[List[AppError], Boolean],
                                  expectedError: List[AppError]) = {
    for {
      log <- ZIO.environment[Has[Logger[String]]].map(_.get)
      service = ConnectedService(
        TestGithubService(githubResult),
        TestTwitterService(twitterResult),
        log
      )
      result <- service.connected(sourceUser, targetUser).flip
    } yield assert(result)(equalTo(expectedError))
  }

  object TestGithubService {
    def apply(result: IO[List[AppError], List[Organization]]) =
      new GithubService.Service {
        override def commonOrganizations(source: User, target: User): IO[List[AppError], List[Organization]] = result
      }
  }

  object TestTwitterService {
    def apply(result: IO[List[AppError], Boolean]) =
      new TwitterService.Service {
        override def friends(source: User, target: User): IO[List[AppError], Boolean] = result
      }
  }
}
