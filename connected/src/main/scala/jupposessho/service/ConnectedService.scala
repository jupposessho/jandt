package jupposessho.service

import jupposessho.model.{AppError, ConnectedResult, User}
import zio._
import zio.logging.Logger

object ConnectedService {

  trait Service {
    def connected(source: User, target: User): IO[List[AppError], ConnectedResult]
  }

  def apply(github: GithubService.Service, twitter: TwitterService.Service, log: Logger[String]) = new Service {

    val logger = log.named("ConnectedService")

    override def connected(source: User, target: User): IO[List[AppError], ConnectedResult] =
      (github
        .commonOrganizations(source, target)
        .either)
        .zipPar(twitter.friends(source, target).either)
        .flatMap {
          case (Right(commonOrganizations), Right(friends)) =>
            val connected = friends && commonOrganizations.nonEmpty
            val commonOrdanizationNames = commonOrganizations.map(_.login)
            log.info(
              s"Connection status for ${source.name} and ${target.name}: $connected / friends: $friends, common organizations: $commonOrdanizationNames"
            ) *>
              ZIO.succeed(ConnectedResult(connected, Option.when(connected)(commonOrdanizationNames)))
          case (Right(_), Left(errors))                  => ZIO.fail(errors)
          case (Left(errors), Right(_))                  => ZIO.fail(errors)
          case (Left(githubErrors), Left(twitterErrors)) => ZIO.fail(githubErrors ++ twitterErrors)
        }
  }
}
