package io.pager.client.github

import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder
import io.circe.generic.semiauto.deriveDecoder
import io.pager.PagerError
import io.pager.PagerError.NotFound
import io.pager.client.github.Live._
import io.pager.client.http.HttpClient
import io.pager.log.Logger
import io.pager.subscription.Repository.{ Name, Version }
import zio.{ IO, ZIO }

private[github] final case class Live(logger: Logger.Service, httpClient: HttpClient.Service) extends GitHubClient.Service {
  override def repositoryExists(name: Name): IO[PagerError, Name] =
    releases(name).as(name)

  override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = {
    val url = s"https://api.github.com/repos/${name.value}/releases"

    logger.info(s"Checking releases of ${name.value}: $url") *>
      httpClient
        .get[List[GitHubRelease]](url)
        .foldM(
          e => logger.warn(s"Couldn't find repository ${name.value}: ${e.message}") *> ZIO.fail(NotFound(name.value)),
          releases => ZIO.succeed(releases)
        )
  }
}

private[github] object Live {
  implicit val versionDecoder: Decoder[Version]             = deriveUnwrappedDecoder
  implicit val gitHubReleaseDecoder: Decoder[GitHubRelease] = deriveDecoder
}
