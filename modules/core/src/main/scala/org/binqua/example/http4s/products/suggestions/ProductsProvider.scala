package org.binqua.example.http4s.products.suggestions

import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeId

object ProductsProvider {

  def default[F[_]: Sync](customProducts: Option[String]): F[Either[String, Products]] = customProducts match {
    case Some(productsAsJson) => FromJsonProductsParser.tryToParse(productsAsJson).pure
    case None =>
      ProductsReader
        .default[F]
        .readFile(fileName = "products.json")
  }

}
