package jupposessho.routes

import jupposessho.model.{AppError, ErrorResponse, User}
import jupposessho.service.ConnectedService
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.implicits._
import zio.Task
import zio.interop.catz._

object ConnectedRoutes {

  trait Service {
    def routes(): HttpApp[Task]
  }

  def apply(service: ConnectedService.Service) = {
    new Service {
      private val dsl = Http4sDsl[Task]

      import dsl._

      def routes(): HttpApp[Task] =
        HttpRoutes
          .of[Task] {
            case GET -> Root / "developers" / "connected" / source / target =>
              for {
                connected <- service.connected(User(source), User(target)).either
                resp <- connected match {
                  case Right(value) => Ok(value)
                  case Left(errors) => handleErrors(errors)
                }
              } yield resp
          }
          .orNotFound

      private def handleErrors(errors: List[AppError]) = {
        val errorResponse = errors.map { error =>
          error match {
            case AppError.GithubUserNotFound(user)  => s"${user.name} is not a valid user in github"
            case AppError.TwitterUserNotFound(user) => s"${user.name} is not a valid user in twitter"
            case AppError.TwitterError(message)     => s"twitter error: $message"
            case AppError.GithubError(message)      => s"github error: $message"
          }
        }
        if (errorResponse.forall(_.contains("is not a valid user in"))) {
          BadRequest(ErrorResponse(errorResponse))
        } else {
          ServiceUnavailable(ErrorResponse(errorResponse))
        }
      }
    }
  }
}
