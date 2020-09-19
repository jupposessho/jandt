package jupposessho.service

import jupposessho.client.GithubClient
import jupposessho.model.{AppError, User}
import jupposessho.model.github.Organization
import org.http4s.client.UnexpectedStatus
import org.http4s.Status.NotFound
import zio._

object GithubService {

  trait Service {
    def commonOrganizations(source: User, target: User): IO[List[AppError], List[Organization]]
  }

  def apply(client: GithubClient.Service) = new Service {

    def commonOrganizations(source: User, target: User): IO[List[AppError], List[Organization]] = {
      (client.organizations(source).either).zipPar(client.organizations(target).either) flatMap {
        handleResult(source, target)
      }
    }

    private def handleResult(source: User, target: User)(
      result: (Either[Throwable, List[Organization]], Either[Throwable, List[Organization]])
    ) =
      result match {
        case (Right(os), Right(ot)) =>
          ZIO.succeed(os.intersect(ot))

        case (Left(sourceError), Left(targetError)) =>
          ZIO.fail(List(convertError(sourceError, source), convertError(targetError, target)))

        case (Right(_), Left(targetError)) =>
          ZIO.fail(List(convertError(targetError, target)))

        case (Left(sourceError), Right(_)) =>
          ZIO.fail(List(convertError(sourceError, source)))
      }

    private def convertError(error: Throwable, user: User): AppError = error match {
      case UnexpectedStatus(NotFound) => AppError.GithubUserNotFound(user)
      case _                          => AppError.GithubError(error.getMessage())
    }
  }
}
