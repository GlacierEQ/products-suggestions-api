package org.binqua.example.http4s.products.suggestions

import cats.effect.IO
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import munit.CatsEffectSuite

class ProductsProviderSpec extends CatsEffectSuite {

  test("given no customProducts then the production products are used") {
    assertIO(
      obtained = ProductsProvider.default[IO](None).map(_.map(_.productSuggestionsMap.size)),
      returns = 10.asRight
    )
  }

  test("given some customProducts then they are used instead of productions product") {

    val productsAsJson =
      """
        |{
        |  "products": [
        |    {"id": "1", "description": "t1", "tags": ["A", "B"]},
        |    {"id": "2", "description": "t2",  "tags": ["A", "B"]}
        |   ]
        |}
        |""".stripMargin

    assertIO(
      obtained = ProductsProvider.default[IO](productsAsJson.some),
      returns = ReferenceData.productsWithProduct1AndProduct2.asRight
    )
  }
}
