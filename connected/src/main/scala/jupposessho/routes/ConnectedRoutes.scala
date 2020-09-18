package jupposessho.routes

import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.implicits._
import zio.Task
import zio.interop.catz._

object ConnectedRoutes {

  trait Service {
    def routes(): HttpApp[Task]
  }

  def apply() = {
    new Service {
      private val dsl = Http4sDsl[Task]

      import dsl._

      def routes(): HttpApp[Task] =
        HttpRoutes
          .of[Task] {
            case GET -> Root =>
              for {
                resp <- Ok("ok")
              } yield resp
          }
          .orNotFound
    }
  }
}
