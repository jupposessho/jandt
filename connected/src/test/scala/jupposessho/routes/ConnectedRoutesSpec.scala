package jupposessho.routes

import org.http4s.implicits._
import org.http4s.{Method, Request}
import zio.interop.catz._
import zio.test._
import zio.test.Assertion.equalTo

object ConnectedRoutesSpec extends DefaultRunnableSpec {

  override def spec = suite("ConnectedRoutes")(
    suite("GET / should respond with")(
      testM("ok") {
        assertM(runConnected())(equalTo("ok"))
      }
    )
  )

  private def runConnected() = {
    for {
      response <- ConnectedRoutes().routes().run(Request(method = Method.GET, uri = uri"/"))
      result <- response.as[String]
    } yield result
  }
}
