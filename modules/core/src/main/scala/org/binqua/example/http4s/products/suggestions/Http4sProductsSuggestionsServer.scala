package org.binqua.example.http4s.products.suggestions

import cats.effect.implicits.effectResourceOps
import cats.effect.{Async, Resource}
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger

object Http4sProductsSuggestionsServer {

  def run[F[_]: Async: Network](): F[Nothing] =
    internalRun(customProducts = None).useForever

  private[suggestions] def internalRun[F[_]: Async: Network](customProducts: Option[String]): Resource[F, Server] =
    ProductsProvider
      .default[F](customProducts)
      .toResource
      .flatMap({
        case Right(validProducts) =>
          createHttpApp(validProducts)
            .flatMap(httpApp =>
              EmberServerBuilder
                .default[F]
                .withHost(ipv4"0.0.0.0")
                .withPort(port"8080")
                .withHttpApp(httpApp)
                .build
            )
        case Left(parserError) =>
          productsParsingFailed(parserError)
      })

  private def productsParsingFailed[F[_]: Async](parserError: String): Resource[F, Nothing] =
    Resource.eval(Async[F].raiseError(new IllegalStateException(parserError)))

  private def createHttpApp[F[_]: Async](products: Products): Resource[F, HttpApp[F]] =
    Resource
      .eval(ProductsSuggestionsPolicy.defaultWithCounter(products))
      .map(policy => Logger.httpApp(logHeaders = true, logBody = true)(httpApp = ProductsSuggestionsApp(policy).app()))
}
