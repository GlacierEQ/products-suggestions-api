package org.binqua.example.http4s.products.suggestions.acceptancetest

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits.{catsSyntaxOptionId, toTraverseOps}
import munit.CatsEffectSuite
import org.binqua.example.http4s.products.suggestions._
import org.http4s.Uri.unsafeFromString
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{Method, Request, Uri}

class Http4sProductsSuggestionsServerSpec extends CatsEffectSuite {

  val allProductionProductIds: List[ProductId] = (1 to 10).map(id => ProductId(id.toString)).toList

  test("Http4sProductsSuggestionsServer: given custom products, it returns the right productId") {
    val productsAsJson =
      """
        |{
        |  "products": [
        |    {"id": "1", "description": "t1", "tags": ["A"]},
        |    {"id": "2", "description": "t2",  "tags": ["A"]},
        |    {"id": "3", "description": "t3", "tags": ["C"]},
        |    {"id": "4", "description": "t4",  "tags": ["C"]}
        |   ]
        |}
        |""".stripMargin

    val actual: Resource[IO, IO[List[(String, Int)]]] = for {
      _ <- Http4sProductsSuggestionsServer.internalRun[IO](productsAsJson.some)
      client <- EmberClientBuilder.default[IO].build
      result <- (1 to 4).toList.traverse(p => client.run(Request[IO](Method.GET, prodUriWithProductId(p.toString))))
    } yield result.map(resp => resp.as[String].map(body => (body, resp.status.code))).sequence

    assertIO(
      obtained = actual.useEval,
      returns = List(("2", 200), ("1", 200), ("4", 200), ("3", 200))
    )
  }

  private def prodUriWithProductId(productId: String): Uri =
    unsafeFromString(s"http://localhost:8080/productSuggestion/$productId")

}
