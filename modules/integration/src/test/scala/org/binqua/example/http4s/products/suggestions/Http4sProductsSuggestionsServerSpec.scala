package org.binqua.example.http4s.products.suggestions

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits.toTraverseOps
import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.munit.TestContainerForAll
import munit.CatsEffectSuite
import org.http4s.Status.{BadRequest, NotFound, Ok}
import org.http4s.Uri.unsafeFromString
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{Method, Request, Response, Status, Uri}
import org.testcontainers.containers.wait.strategy.Wait

class Http4sProductsSuggestionsServerSpec extends CatsEffectSuite with TestContainerForAll {

  override val containerDef: GenericContainer.Def[GenericContainer] = GenericContainer.Def(
    "binqua-products-suggestions/web-app:0.2",
    exposedPorts = Seq(8080),
    waitStrategy = Wait.forListeningPort
  )

  test("The first request to the GET request to http://localhost:<port>/productSuggestion/1 returns product id 89") {
    withContainers { implicit container =>
      withDefaultClient()(client => {
        val actualSuggestedProductId: IO[String] = client.expect[String](uri = prodUriWithProductId("1"))

        assertIO(obtained = actualSuggestedProductId, returns = "4")
      })
    }
  }

  test("GET request to http://localhost:<port>/productSuggestion/ returns 400 - Bad Request with no body") {

    withContainers { implicit container =>
      withDefaultClient()(client => {
        val response: Resource[IO, Response[IO]] =
          client.run(Request[IO](Method.GET, prodUriWithProductId(productId = "")))

        assertIO(
          obtained = response.use(resp => resp.as[String].map(body => (body, resp.status.code))),
          returns = ("", 400)
        )
      })

    }
  }

  test(
    "GET request to http://localhost:<port>/productSuggestion/12347 returns not found - 404 and body 'Not found'"
  ) {
    withContainers { implicit container =>
      withDefaultClient()(client => {
        val response: Resource[IO, Response[IO]] =
          client.run(Request[IO](Method.GET, prodUriWithProductId("12347")))

        assertIO(
          obtained = response.use(resp => resp.as[String].map(body => (body, resp.status.code))),
          returns = ("", 404)
        )
      })
    }
  }

  test(
    "http://.../productSuggestion/<id> for non existing productId returns BadRequest or NotFound: app keeps working and does not crash"
  ) {

    type BodyAsString = String
    def extractSingleResponseResult: ((Response[IO], ProductId)) => IO[(BodyAsString, Status, ProductId)] =
      in => {
        val (singleRes, productId) = in
        singleRes.as[String].map(body => (body, singleRes.status, productId))
      }

    def similarproductIdFor(productId: ProductId)(implicit container: GenericContainer): Request[IO] =
      Request[IO](Method.GET, prodUriWithProductId(productId.id))

    val maxProductIdAvailable = 10
    val productIdRequested = (1 to maxProductIdAvailable + 1).toList
      .map(id => ProductId(id.toString))
      .prepended(ProductId(""))

    withContainers { implicit container =>
      withDefaultClient(maxIdleConnections = productIdRequested.size + 20)(client => {
        val resourceResponses: Resource[IO, List[(Response[IO], ProductId)]] =
          productIdRequested
            .traverse(productId => client.run(similarproductIdFor(productId)).map((_, productId)))

        val actualNonOkResponses: IO[List[(BodyAsString, Status, ProductId)]] =
          resourceResponses.use(responses => {
            responses
              .map(extractSingleResponseResult)
              .sequence
              .map(_.partition(_._2 != Ok)._1)

          })
        assertIO(
          obtained = actualNonOkResponses,
          returns = List(
            ("", BadRequest, ProductId("")),
            ("", NotFound, ProductId("11"))
          )
        )
      })
    }

  }
  private def prodUriWithProductId(productId: String)(implicit container: GenericContainer): Uri =
    unsafeFromString(
      s"http://localhost:${container.mappedPort(8080)}/productSuggestion/$productId"
    )

  private def withDefaultClient(maxIdleConnections: Int = 100)(f: Client[IO] => IO[Unit]): IO[Unit] = {
    EmberClientBuilder
      .default[IO]
      .withMaxTotal(maxIdleConnections)
      .build
      .use(f)
  }

}
