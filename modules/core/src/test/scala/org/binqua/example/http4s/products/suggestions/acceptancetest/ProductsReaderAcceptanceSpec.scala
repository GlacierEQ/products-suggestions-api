package org.binqua.example.http4s.products.suggestions.acceptancetest

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import munit.CatsEffectSuite
import org.binqua.example.http4s.products.suggestions.ProductsReader

class ProductsReaderAcceptanceSpec extends CatsEffectSuite {

  test("ProductsReader can read production products.json: there are 10 products") {
    assertIO(
      obtained = ProductsReader
        .default[IO]
        .readFile("products.json")
        .map(parsingOutcome => parsingOutcome.map(products => products.productSuggestionsMap.size)),
      returns = 10.asRight
    )
  }
}
