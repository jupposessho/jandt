package jupposessho.service

import jupposessho.model.{AppError, ConnectedResult, User}
import zio._

object ConnectedService {

  trait Service {
    def connected(source: User, target: User): IO[List[AppError], ConnectedResult]
  }

  def apply(github: GithubService.Service, twitter: TwitterService.Service) = new Service {

    override def connected(source: User, target: User): IO[List[AppError], ConnectedResult] =
      (github
        .commonOrganizations(source, target)
        .either)
        .zipPar(twitter.friends(source, target).either)
        .flatMap {
          case (Right(commonOrganizations), Right(friends)) =>
            val connected = friends && commonOrganizations.nonEmpty
            ZIO.succeed(ConnectedResult(connected, Option.when(connected)(commonOrganizations.map(_.login))))
          case (Right(_), Left(errors))                  => ZIO.fail(errors)
          case (Left(errors), Right(_))                  => ZIO.fail(errors)
          case (Left(githubErrors), Left(twitterErrors)) => ZIO.fail(githubErrors ++ twitterErrors)
        }
  }
}
