package org.binqua.example.http4s.products.suggestions

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import munit.CatsEffectSuite
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Response}

class ProductsSuggestionsAppSpec extends CatsEffectSuite {

  test(
    "GET request to /productSuggestion/1: given productId 2 with same tag of productId 1, then productId2 is returned"
  ) {

    def policy(expected: ProductId, returned: ProductId): ProductsSuggestionsPolicy[IO] =
      new ProductsSuggestionsPolicy[IO] {
        override def findASuggestion(productId: ProductId): IO[Option[ProductId]] =
          if (productId == expected) IO.pure(returned.some) else IO.pure(None)

        override def counter(productId: ProductId): IO[Option[Int]] = TestUtils.thisCodeShouldNotBeExercised()

      }

    val result: IO[Response[IO]] =
      ProductsSuggestionsApp(policy(expected = ReferenceData.productId1, returned = ReferenceData.productId2))
        .app()
        .run(Request[IO](Method.GET, uri"/productSuggestion/1"))

    assertIO(
      obtained = result.flatMap(resp => resp.as[String].map(body => (body, resp.status.code))),
      returns = (ReferenceData.productId2.id, 200)
    )

  }

  test(
    "GET request to /productSuggestion/1: given a policy cannot find a similar product then it returns 404"
  ) {

    val cannotFindAnyProduct: ProductsSuggestionsPolicy[IO] = new ProductsSuggestionsPolicy[IO] {
      override def findASuggestion(productId: ProductId): IO[Option[ProductId]] = IO.pure(None)

      override def counter(productId: ProductId): IO[Option[Int]] = TestUtils.thisCodeShouldNotBeExercised()

    }

    val result: IO[Response[IO]] =
      ProductsSuggestionsApp(cannotFindAnyProduct)
        .app()
        .run(Request[IO](Method.GET, uri"/productSuggestion/1"))

    assertIO(
      obtained = result.map(resp => resp.status.code),
      returns = 404
    )

  }

  test("a request to an endpoint different from GET /productSuggestion/<id> returns 404 - Not Found") {

    val response: IO[Response[IO]] =
      ProductsSuggestionsApp(policy = notUsed)
        .app()
        .run(Request[IO](Method.GET, uri"/bla/bla"))

    assertIO(
      obtained = response.flatMap(resp => resp.as[String].map(body => (body, resp.status.code))),
      returns = ("Not found", 404)
    )

  }

  test(
    "GET request to /productSuggestion/: a request with no product id specified returns 400 - bad request"
  ) {

    val response: IO[Response[IO]] =
      ProductsSuggestionsApp(policy = notUsed)
        .app()
        .run(Request[IO](Method.GET, uri"/productSuggestion/"))

    assertIO(
      obtained = response.map(_.status.code),
      returns = 400
    )

  }

  test(
    "GET request to /totProductSuggestion/: return the right value of the counter"
  ) {

    def policy(expected: ProductId, returnedCounter: Int): ProductsSuggestionsPolicy[IO] =
      new ProductsSuggestionsPolicy[IO] {
        override def findASuggestion(productId: ProductId): IO[Option[ProductId]] =
          TestUtils.thisCodeShouldNotBeExercised()
        override def counter(productId: ProductId): IO[Option[Int]] =
          if (productId == expected) IO.pure(returnedCounter.some) else IO.pure(None)

      }

    val response: IO[(String, Int)] =
      ProductsSuggestionsApp(policy = policy(ReferenceData.productId1, 10))
        .app()
        .run(Request[IO](Method.GET, uri"/totProductSuggestion/1"))
        .flatMap(resp => resp.as[String].map(body => (body, resp.status.code)))

    assertIO(
      obtained = response,
      returns = ("10", 200)
    )

  }

  def notUsed: ProductsSuggestionsPolicy[IO] = new ProductsSuggestionsPolicy[IO] {
    override def findASuggestion(productId: ProductId): IO[Option[ProductId]] = throw new IllegalStateException(
      "This code Should not be exercised in this test"
    )

    override def counter(productId: ProductId): IO[Option[Int]] = TestUtils.thisCodeShouldNotBeExercised()

  }

}
