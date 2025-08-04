package org.binqua.example.http4s.products.suggestions

import cats.Monad
import cats.data.OptionT
import cats.effect.std.Random
import cats.effect.{Ref, Sync}
import cats.implicits._

object ProductsSuggestionsPolicy {

  def defaultWithCounter[F[_]: Sync](products: Products): F[ProductsSuggestionsPolicy[F]] =
    for {
      random <- Random.scalaUtilRandomSeedLong[F](1234L)
      productsRef <- Ref.of[F, Products](products)
    } yield new RandomTagBasedProductsSuggestionsPolicyWithCounter[F](random, productsRef)

}

trait ProductsSuggestionsPolicy[F[_]] {
  def findASuggestion(productId: ProductId): F[Option[ProductId]]
  def counter(productId: ProductId): F[Option[Int]]
}

private class RandomTagBasedProductsSuggestionsPolicyWithCounter[F[_]: Monad](
    random: Random[F],
    refProducts: Ref[F, Products]
) extends ProductsSuggestionsPolicy[F] {
  override def findASuggestion(productId: ProductId): F[Option[ProductId]] = for {
    productWithNewCounter <- refProducts.updateAndGet(products => products.newWithCounterIncreased(productId))
    result <- findSuggestionInternal(productId, productWithNewCounter)
  } yield result

  private def findSuggestionInternal(productId: ProductId, products: Products): F[Option[ProductId]] = (for {
    suggestionsIds <- OptionT.fromOption[F](products.findSuggestedProductIds(productId))
    theSuggestionFound <- OptionT(
      random.nextIntBounded(suggestionsIds.size).map(index => suggestionsIds.toList(index).some)
    )
  } yield theSuggestionFound).value

  override def counter(productId: ProductId): F[Option[Int]] = refProducts.get.map(_.productsCounter.get(productId))
}
