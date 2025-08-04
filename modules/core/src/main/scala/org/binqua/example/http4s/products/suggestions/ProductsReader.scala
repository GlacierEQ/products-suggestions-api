package org.binqua.example.http4s.products.suggestions

import cats.effect.{Resource, Sync}
import cats.implicits.toBifunctorOps
import io.circe

import scala.io.Source

trait ProductsReader[F[_]] {
  def readFile(fileName: String): F[Either[String, Products]]
}

object ProductsReader {
  def default[F[_]: Sync]: ProductsReader[F] = new SimpleProductsReader[F](productsParser = FromJsonProductsParser)
}

private class SimpleProductsReader[F[_]: Sync](productsParser: ProductsParser) extends ProductsReader[F] {
  override def readFile(fileName: String): F[Either[String, Products]] =
    Resource
      .make(acquire = Sync[F].blocking(Source.fromResource(fileName)))(file => Sync[F].delay(file.close()))
      .map(_.mkString)
      .map(productsParser.tryToParse)
      .use(Sync[F].pure)
}

trait ProductsParser {
  def tryToParse(rawProducts: String): Either[String, Products]
}

private object FromJsonProductsParser extends ProductsParser {

  override def tryToParse(rawProducts: String): Either[String, Products] =
    circe.parser
      .decode[RawProducts](rawProducts)
      .leftMap(e => toError(e.toString))
      .flatMap(rawProducts => Products.from(rawProducts.products).leftMap(toError))

  private def toError(details: String): String =
    s"RawProducts cannot be parsed successfully. Details:\n$details"
}
