package jupposessho.service

import jupposessho.TestDependencies
import jupposessho.client.GithubClient
import jupposessho.model.{AppError, User}
import jupposessho.model.github.Organization
import org.http4s.client._
import org.http4s.Status.{BadRequest, NotFound}
import zio._
import zio.logging.Logger
import zio.test._
import zio.test.Assertion._

object GithubServiceSpec extends DefaultRunnableSpec {

  val sourceUser = User("source")
  val targetUser = User("target")
  val organization = Organization(1, "login", "url")

  override def spec =
    suite("GithubService.commonOrganizations should")(
      suite("return the common organizations")(
        testM("when the given accounts are exist") {
          val otherOrganization = Organization(2, "other", "url2")
          val sourceOrgs = List(
            Organization(3, "other", "url3"),
            organization,
            otherOrganization
          )
          val targetOrgs = List(
            organization,
            Organization(4, "other", "url4"),
            otherOrganization
          )
          val client = TestGithubClient(Task.succeed(sourceOrgs), Task.succeed(targetOrgs))
          for {
            log <- ZIO.environment[Has[Logger[String]]].map(_.get)
            result <- GithubService(client, log).commonOrganizations(sourceUser, targetUser)
          } yield assert(result)(equalTo(List(organization, otherOrganization)))
        },
        testM("when there is no common organization") {
          val sourceOrgs = List(Organization(3, "other", "url3"))
          val targetOrgs = List(Organization(4, "other", "url4"))
          val client = TestGithubClient(Task.succeed(sourceOrgs), Task.succeed(targetOrgs))
          for {
            log <- ZIO.environment[Has[Logger[String]]].map(_.get)
            result <- GithubService(client, log).commonOrganizations(sourceUser, targetUser)
          } yield assert(result)(equalTo(Nil))
        }
      ),
      suite("fail")(
        testM("when both users are missing") {
          assertFailure(
            Task.fail(UnexpectedStatus(NotFound)),
            Task.fail(UnexpectedStatus(NotFound)),
            List(AppError.GithubUserNotFound(sourceUser), AppError.GithubUserNotFound(targetUser))
          )
        },
        testM("when source user is missing") {
          assertFailure(Task.fail(UnexpectedStatus(NotFound)),
                        Task.succeed(List(organization)),
                        List(AppError.GithubUserNotFound(sourceUser)))
        },
        testM("when target user is missing") {
          assertFailure(Task.succeed(List(organization)),
                        Task.fail(UnexpectedStatus(NotFound)),
                        List(AppError.GithubUserNotFound(targetUser)))
        },
        testM("when both calls fail with other error") {
          assertFailure(
            Task.fail(new Exception("source message")),
            Task.fail(new Exception("target message")),
            List(AppError.GithubError("source message"), AppError.GithubError("target message"))
          )
        },
        testM("when source call fails with other error") {
          assertFailure(Task.fail(new Exception("source message")),
                        Task.succeed(List(organization)),
                        List(AppError.GithubError("source message")))
        },
        testM("when target call fails with other error") {
          assertFailure(Task.succeed(List(organization)),
                        Task.fail(new Exception("target message")),
                        List(AppError.GithubError("target message")))
        },
        testM("when source call fails with other error and target user not found") {
          assertFailure(
            Task.fail(new Exception("source message")),
            Task.fail(UnexpectedStatus(NotFound)),
            List(AppError.GithubError("source message"), AppError.GithubUserNotFound(targetUser))
          )
        },
        testM("when target call fails with other error and source user not found") {
          assertFailure(
            Task.fail(UnexpectedStatus(NotFound)),
            Task.fail(new Exception("target message")),
            List(AppError.GithubUserNotFound(sourceUser), AppError.GithubError("target message"))
          )
        },
        testM("when target call fails with other error and source fails with other UnexpectedStatus error") {
          val error = UnexpectedStatus(BadRequest)
          assertFailure(Task.fail(error),
                        Task.fail(new Exception("target message")),
                        List(AppError.GithubError(error.getMessage()), AppError.GithubError("target message")))
        }
      )
    ).provideCustomLayer(TestDependencies.logger("GithubServiceSpec"))

  private def assertFailure(sourceResult: Task[List[Organization]],
                            targetResult: Task[List[Organization]],
                            expectedErrors: List[AppError]) = {
    val client = TestGithubClient(sourceResult, targetResult)
    for {
      log <- ZIO.environment[Has[Logger[String]]].map(_.get)
      result <- GithubService(client, log).commonOrganizations(sourceUser, targetUser).flip
    } yield assert(result)(equalTo(expectedErrors))
  }

  object TestGithubClient {
    def apply(sourceResult: Task[List[Organization]], targetResult: Task[List[Organization]]) =
      new GithubClient.Service {
        override def organizations(user: User): Task[List[Organization]] =
          if (user == sourceUser) sourceResult else targetResult
      }
  }
}
