package org.binqua.example.http4s.products.suggestions

import cats.data.NonEmptyList
import cats.implicits.{catsSyntaxOptionId, catsSyntaxParallelSequence1}
import cats.kernel.Monoid
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

object RawProducts {
  implicit val decoder: Decoder[RawProducts] = deriveDecoder[RawProducts]
}
case class RawProducts(products: List[Product])

object Products {

  def from(rawProduct: List[Product]): Either[String, Products] =
    Products.buildProductSuggestionsMap(rawProduct).map(new Products(_, Map.empty) {})

  def unsafeFrom(rawProducts: List[Product]): Products =
    Products.from(rawProducts).fold[Products](e => throw new IllegalArgumentException(e), identity)

  private def findProductWithTag(tag: Tag, products: List[Product]): List[ProductId] =
    products.filter(_.tags.toList.contains(tag)).map(_.id)

  private def buildProductSuggestionsMap(
      products: List[Product]
  ): Either[String, Map[ProductId, NonEmptyList[ProductId]]] = {
    implicit val onePerLine: Monoid[String] = new Monoid[String] {
      override def empty: String = ""

      override def combine(x: String, y: String): String = x + "\n" + y
    }
    products
      .map(product =>
        NonEmptyList
          .fromList(product.tags.toList.flatMap(findProductWithTag(_, products)).filter(_ != product.id).distinct)
          .toRight(s"Product id ${product.id.id} does not have similar products")
          .map((product.id, _))
      )
      .parSequence
      .map(_.toMap)
  }

}

abstract case class Products(
    productSuggestionsMap: Map[ProductId, NonEmptyList[ProductId]],
    productsCounter: Map[ProductId, Int]
) {

  def findSuggestedProductIds(productId: ProductId): Option[NonEmptyList[ProductId]] =
    productSuggestionsMap.get(productId)

  def newWithCounterIncreased(productId: ProductId): Products = productSuggestionsMap
    .get(productId)
    .map(_ => {
      val counterUpdated = productsCounter.updatedWith(productId)(_.map(_ + 1).getOrElse(1).some)
      new Products(productSuggestionsMap, counterUpdated) {}

    })
    .getOrElse(this)

}

object ProductId {
  implicit val decoder: Decoder[ProductId] = Decoder[String].map(ProductId(_))
}

case class ProductId(id: String) extends AnyVal

object ProductDesc {
  implicit val decoder: Decoder[ProductDesc] = Decoder[String].map(ProductDesc(_))
}

case class ProductDesc(description: String) extends AnyVal

object Tag {
  implicit val decoder: Decoder[Tag] = Decoder[String].map(Tag(_))
}

case class Tag(value: String) extends AnyVal

object Product {
  implicit val encoder: Decoder[Product] = deriveDecoder[Product]
}

case class Product(
    id: ProductId,
    description: ProductDesc,
    tags: NonEmptyList[Tag]
)
