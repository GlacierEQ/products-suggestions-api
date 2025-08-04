package org.binqua.example.http4s.products.suggestions

import cats.effect.Concurrent
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpApp, HttpRoutes}

private[suggestions] case class ProductsSuggestionsApp[F[_]: Concurrent](policy: ProductsSuggestionsPolicy[F])
    extends Http4sDsl[F] {

  def app(): HttpApp[F] =
    HttpRoutes
      .of[F] {
        case GET -> Root / "productSuggestion" / productId =>
          if (productId.isEmpty)
            BadRequest()
          else
            policy
              .findASuggestion(productId = ProductId(productId))
              .flatMap {
                case Some(productId) => Ok(productId.id)
                case None            => NotFound()
              }

        case GET -> Root / "totProductSuggestion" / productId =>
          if (productId.isEmpty)
            BadRequest()
          else
            policy
              .counter(productId = ProductId(productId))
              .flatMap {
                case Some(counter) => Ok(counter.toString)
                case None          => NotFound()
              }
      }
      .orNotFound

}
