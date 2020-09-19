package jupposessho.routes

import jupposessho.model.{AppError, ConnectedResult, User}
import jupposessho.service.ConnectedService
import org.http4s.{Method, Request, Status, Uri}
import org.http4s.implicits._
import zio._
import zio.interop.catz._
import zio.test._
import zio.test.Assertion.equalTo

object ConnectedRoutesSpec extends DefaultRunnableSpec {

  override def spec = suite("GET / should respond with")(
    suite("ConnectedResult")(
      testM("when connected") {
        val organizationName = "login"
        val connectedResult = ConnectedResult(true, Some(List(organizationName)))
        val service = TestConnectedService(IO.succeed(connectedResult))
        val expectedResponse = s"""{"connected":true,"organisations":["$organizationName"]}"""

        assertM(runConnected(service))(equalTo(Status.Ok -> expectedResponse))
      },
      testM("when not connected") {
        val connectedResult = ConnectedResult(false, None)
        val service = TestConnectedService(IO.succeed(connectedResult))
        val expectedResponse = """{"connected":false}"""

        assertM(runConnected(service))(equalTo(Status.Ok -> expectedResponse))
      }
    ),
    suite("fail")(
      testM("with github user not found") {
        val service = TestConnectedService(IO.fail(List(AppError.GithubUserNotFound(User("source")))))
        val expectedResponse = """{"errors":["source is not a valid user in github"]}"""

        assertM(runConnected(service))(equalTo(Status.BadRequest -> expectedResponse))
      },
      testM("with twitter user not found") {
        val service = TestConnectedService(IO.fail(List(AppError.TwitterUserNotFound(User("source")))))
        val expectedResponse = """{"errors":["source is not a valid user in twitter"]}"""

        assertM(runConnected(service))(equalTo(Status.BadRequest -> expectedResponse))
      },
      testM("with other twitter error") {
        val service = TestConnectedService(IO.fail(List(AppError.TwitterError("oops"))))
        val expectedResponse = """{"errors":["twitter error: oops"]}"""

        assertM(runConnected(service))(equalTo(Status.ServiceUnavailable -> expectedResponse))
      },
      testM("with other github error") {
        val service = TestConnectedService(IO.fail(List(AppError.GithubError("oops"))))
        val expectedResponse = """{"errors":["github error: oops"]}"""

        assertM(runConnected(service))(equalTo(Status.ServiceUnavailable -> expectedResponse))
      },
      testM("with not only user not found error") {
        val service = TestConnectedService(
          IO.fail(List(AppError.TwitterUserNotFound(User("source")), AppError.GithubError("oops")))
        )
        val expectedResponse = """{"errors":["source is not a valid user in twitter","github error: oops"]}"""

        assertM(runConnected(service))(equalTo(Status.ServiceUnavailable -> expectedResponse))
      },
      testM("with users not found") {
        val service = TestConnectedService(
          IO.fail(
            List(
              AppError.GithubUserNotFound(User("source")),
              AppError.GithubUserNotFound(User("target")),
              AppError.TwitterUserNotFound(User("source")),
              AppError.TwitterUserNotFound(User("target"))
            )
          )
        )
        val expectedResponse =
          """{"errors":[
            |"source is not a valid user in github",
            |"target is not a valid user in github",
            |"source is not a valid user in twitter",
            |"target is not a valid user in twitter"
            |]}""".stripMargin.replace("\n", "")

        assertM(runConnected(service))(equalTo(Status.BadRequest -> expectedResponse))
      }
    )
  )

  private def runConnected(service: ConnectedService.Service) = {
    for {
      response <- ConnectedRoutes(service)
        .routes()
        .run(
          Request(method = Method.GET, uri = Uri.fromString(s"/developers/connected/source/target").getOrElse(uri"/"))
        )
      responseStatus = response.status
      responseBody <- response.as[String]
    } yield (responseStatus, responseBody)
  }

  object TestConnectedService {
    def apply(result: IO[List[AppError], ConnectedResult],
              sourceUserName: String = "source",
              targetUserName: String = "target") =
      new ConnectedService.Service {
        override def connected(source: User, target: User): IO[List[AppError], ConnectedResult] =
          if (sourceUserName == source.name && targetUserName == target.name) result
          else
            IO.die(new Exception("test failed: connected should be called with parameters from the request"))
      }
  }
}
