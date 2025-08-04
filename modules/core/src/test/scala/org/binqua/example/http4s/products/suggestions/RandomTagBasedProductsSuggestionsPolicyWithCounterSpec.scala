package org.binqua.example.http4s.products.suggestions

import cats.effect.std.Random
import cats.effect.{IO, Ref}
import cats.implicits.{catsSyntaxOptionId, catsSyntaxParallelAp1, toTraverseOps}
import munit.CatsEffectSuite

class RandomTagBasedProductsSuggestionsPolicyWithCounterSpec extends CatsEffectSuite {

  test("given we call findASuggestion for productId1 10 times counter has to be 10") {
    val expectedProductId1Counter = 10
    val expectedProductId2Counter = 5

    val actualResult: IO[(Option[Set[ProductId]], Option[Set[ProductId]], Map[ProductId, Int])] = for {
      random <- Random.scalaUtilRandomSeedLong[IO](seed = 1)
      ref <- Ref.of[IO, Products](ReferenceData.productsWithProduct1AndProduct2)
      policyUnderTest <- IO(new RandomTagBasedProductsSuggestionsPolicyWithCounter[IO](random, ref))
      actualSuggestionsFor1 <- policyUnderTest
        .findASuggestion(ReferenceData.productId1)
        .parReplicateA(expectedProductId1Counter)
      actualSuggestionsFor2 <- policyUnderTest
        .findASuggestion(ReferenceData.productId2)
        .parReplicateA(expectedProductId2Counter)
      actualCounter <- ref.get.map(_.productsCounter)
    } yield {
      (actualSuggestionsFor1.sequence.map(_.toSet), actualSuggestionsFor2.sequence.map(_.toSet), actualCounter)
    }

    assertIO(
      actualResult,
      (
        Set(ReferenceData.productId2).some,
        Set(ReferenceData.productId1).some,
        Map(
          ReferenceData.productId1 -> expectedProductId1Counter,
          ReferenceData.productId2 -> expectedProductId2Counter
        )
      )
    )

  }

  test("given a non existing productId findASuggestion return none and counter is not updated") {
    val actualResult: IO[(Option[Set[ProductId]], Map[ProductId, Int])] = for {
      random <- Random.scalaUtilRandomSeedInt[IO](seed = 1)
      ref <- Ref.of[IO, Products](ReferenceData.productsWithProduct1AndProduct2)
      policyUnderTest <- IO(new RandomTagBasedProductsSuggestionsPolicyWithCounter[IO](random, ref))
      actualSuggestionIds <- policyUnderTest
        .findASuggestion(ProductId("I dont exist"))
        .replicateA(10)
      actualCounter <- ref.get.map(_.productsCounter)
    } yield {
      (actualSuggestionIds.sequence.map(_.toSet), actualCounter)
    }

    assertIO(actualResult, (None, Map.empty[ProductId, Int]))

  }

}
