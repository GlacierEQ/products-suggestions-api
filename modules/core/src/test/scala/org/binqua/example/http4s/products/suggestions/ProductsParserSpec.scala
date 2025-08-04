package org.binqua.example.http4s.products.suggestions

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxEitherId
import io.circe
import munit.FunSuite

class ProductsParserSpec extends FunSuite {

  test("Given a valid Product encoded as json, decode can parse it successfully") {
    assertEquals(
      obtained = circe.parser
        .decode[Product](input = """{"id": "1", "description": "t1", "tags": ["A"]}""".stripMargin),
      expected = Product(
        id = ProductId("1"),
        description = ProductDesc("t1"),
        tags = NonEmptyList.of(Tag("A"))
      ).asRight
    )
  }

  test("Given a valid rawProducts encoded as json, decode can parse it successfully") {
    val aValidRawProductsJson =
      """
        |{
        |  "products": [
        |    {"id": "1", "description": "t1", "tags": ["A", "B"]},
        |    {"id": "2", "description": "t2",  "tags": ["A", "B"]}
        |   ]
        |}
        |""".stripMargin

    assertEquals(
      obtained = FromJsonProductsParser
        .tryToParse(aValidRawProductsJson)
        .map(_.productSuggestionsMap),
      expected = Map(
        ReferenceData.productId1 -> NonEmptyList.of(ReferenceData.productId2),
        ReferenceData.productId2 -> NonEmptyList.of(ReferenceData.productId1)
      ).asRight
    )

  }

  test(
    "Given a valid rawProducts encoded as json with 4 entries and tags with no matching productId, decode can parse it successfully"
  ) {
    val aValidRawProductsJson =
      """
        |{
        |  "products": [
        |    {"id": "1", "description": "t1", "length": 1, "tags": ["A", "B", "Z"]},
        |    {"id": "2", "description": "t2", "length": 2, "tags": ["A", "B", "Y"]},
        |    {"id": "3", "description": "t2", "length": 2, "tags": ["C", "D", "X"]},
        |    {"id": "4", "description": "t2", "length": 2, "tags": ["C", "D", "H"]}
        |   ]
        |}
        |""".stripMargin

    assertEquals(
      obtained = FromJsonProductsParser
        .tryToParse(rawProducts = aValidRawProductsJson)
        .map(_.productSuggestionsMap),
      expected = Map(
        ReferenceData.productId1 -> NonEmptyList.of(ReferenceData.productId2),
        ReferenceData.productId2 -> NonEmptyList.of(ReferenceData.productId1),
        ProductId("3") -> NonEmptyList.of(ProductId("4")),
        ProductId("4") -> NonEmptyList.of(ProductId("3"))
      ).asRight
    )

  }

  test("Given an invalid products json content, decode fails to parse it") {
    assertEquals(
      obtained = FromJsonProductsParser.tryToParse(rawProducts = """{"id":}"""),
      expected =
        "RawProducts cannot be parsed successfully. Details:\nParsingFailure: expected json value got '}' (line 1, column 7)".asLeft
    )
  }

  test("Given a products with a product with no tags, decode fails to parse it") {
    val anInvalidRawProductsJson =
      """
        |{
        |  "products": [
        |    {"id": "1", "description": "t1", "length": 1, "tags": ["A", "B"]},
        |    {"id": "2", "description": "t2", "length": 2, "tags": []}
        |   ]
        |}
        |""".stripMargin

    assertEquals(
      obtained = FromJsonProductsParser.tryToParse(anInvalidRawProductsJson),
      expected =
        "RawProducts cannot be parsed successfully. Details:\nDecodingFailure at .products[1][0]: Couldn't decode products[1][0]".asLeft
    )
  }

  test(
    "product 1 with tag A and B and product 2 with tag A is still valid even if tag B has no products because tag A has 2 products"
  ) {
    val aStrangeButValidRawJson =
      """
        |{
        |  "products": [
        |    {"id": "1", "description": "t1", "tags": ["A", "B"]},
        |    {"id": "2", "description": "t2", "tags": ["A"]}
        |   ]
        |}
        |""".stripMargin

    assertEquals(
      obtained = FromJsonProductsParser
        .tryToParse(aStrangeButValidRawJson)
        .map(_.productSuggestionsMap),
      expected = Map(
        ReferenceData.productId1 -> NonEmptyList.of(ReferenceData.productId2),
        ReferenceData.productId2 -> NonEmptyList.of(ReferenceData.productId1)
      ).asRight
    )
  }

  test(
    "Given rawProducts with product 1 with tag A and B, product 2 with tag A and product 3 with tag C, it is not valid because product 3 has a tag with only 1 product"
  ) {
    val invalidJson =
      """
        |{
        |  "products": [
        |    {"id": "1", "description": "t1", "length": 1, "tags": ["A", "B"]},
        |    {"id": "2", "description": "t2", "length": 2, "tags": ["A"]},
        |    {"id": "3", "description": "t3", "length": 2, "tags": ["C"]},
        |    {"id": "4", "description": "t4", "length": 2, "tags": ["D"]}
        |   ]
        |}
        |""".stripMargin

    assertEquals(
      obtained = FromJsonProductsParser.tryToParse(invalidJson),
      expected =
        "RawProducts cannot be parsed successfully. Details:\nProduct id 3 does not have similar products\nProduct id 4 does not have similar products".asLeft
    )
  }

  test("Given rawProducts with one product is not valid because its tags cannot have other products") {
    val json = """{  "products": [ {"id": "1", "description": "t1", "length": 1, "tags": ["B"]}  ]} """.stripMargin

    assertEquals(
      obtained = FromJsonProductsParser.tryToParse(rawProducts = json),
      expected =
        "RawProducts cannot be parsed successfully. Details:\nProduct id 1 does not have similar products".asLeft
    )
  }

}
