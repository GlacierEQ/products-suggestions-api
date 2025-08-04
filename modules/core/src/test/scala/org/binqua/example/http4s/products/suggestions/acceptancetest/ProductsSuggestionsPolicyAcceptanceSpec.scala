package org.binqua.example.http4s.products.suggestions.acceptancetest

import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.{catsSyntaxOptionId, catsSyntaxTuple2Semigroupal, toTraverseOps}
import munit.CatsEffectSuite
import org.binqua.example.http4s.products.suggestions._

class ProductsSuggestionsPolicyAcceptanceSpec extends CatsEffectSuite {

  val allProductionProductIds: List[ProductId] = (1 to 10).map(id => ProductId(id.toString)).toList

  test(
    "Prod ProductsSuggestionsPolicy: given custom products, it returns the right productId and the right counter value of 1"
  ) {
    val actual: IO[Option[(ProductId, Int)]] = for {
      policy <- ProductsSuggestionsPolicy.defaultWithCounter[IO](ReferenceData.productsWithProduct1AndProduct2)
      suggestionId <- policy.findASuggestion(ReferenceData.productId1)
      product1Counter <- policy.counter(ReferenceData.productId1)
    } yield (suggestionId, product1Counter).tupled

    assertIO(
      obtained = actual,
      returns = (ReferenceData.productId2, 1).some
    )
  }

  test("Prod ProductsSuggestionsPolicy: counter return right amount") {

    val actual: IO[Boolean] =
      for {
        expectedCounter <- Random.scalaUtilRandom[IO].flatMap(_.betweenInt(0, 20))
        policy <- ProductsSuggestionsPolicy.defaultWithCounter[IO](ReferenceData.productsWithProduct1AndProduct2)
        _ <- policy.findASuggestion(ReferenceData.productId1).replicateA(expectedCounter)
        _ <- policy.findASuggestion(ReferenceData.productId2).replicateA(expectedCounter)
        product1Counter <- policy.counter(ReferenceData.productId1)
        product2Counter <- policy.counter(ReferenceData.productId1)
      } yield (product1Counter, product2Counter).tupled == (expectedCounter, expectedCounter).some

    assertIO(
      obtained = actual,
      returns = true
    )
  }

  test(
    "Prod ProductsSuggestionsPolicy: using the given products.json, can run 50 times for each of the 10 products and it returns all productIds. It never fails."
  ) {

    def runFor(
        productIds: List[ProductId],
        numberOfIterationForEachProductId: Int,
        policy: ProductsSuggestionsPolicy[IO]
    ): IO[List[Option[ProductId]]] =
      productIds
        .traverse(ProductId => policy.findASuggestion(ProductId).replicateA(numberOfIterationForEachProductId))
        .map(_.flatten)

    case class Expectation(numberOfErrors: Int, allProductIdsFound: Set[ProductId])

    def extractProductIds(goodOutcome: List[Option[ProductId]]): Set[ProductId] =
      goodOutcome
        .map(_.getOrElse(throw new IllegalStateException("we should have only good outcome")))
        .sortBy(_.id)
        .toSet

    val actualExpectation: IO[Expectation] = for {
      productsParserResult <- ProductsReader.default[IO].readFile("products.json")
      productionPolicy <- productsParserResult match {
        case Left(error)     => TestUtils.thisCodeShouldNotBeExercised(error)
        case Right(products) => ProductsSuggestionsPolicy.defaultWithCounter[IO](products)
      }
      actualExpectation <- runFor(
        productIds = allProductionProductIds,
        numberOfIterationForEachProductId = 20,
        policy = productionPolicy
      ).map(_.partition(_.isEmpty))
        .map({ case (idsNotFound, idsFound) =>
          Expectation(idsNotFound.size, extractProductIds(idsFound))
        })
    } yield actualExpectation

    assertIO(
      obtained = actualExpectation,
      returns = Expectation(numberOfErrors = 0, allProductIdsFound = allProductionProductIds.toSet)
    )

  }

}
