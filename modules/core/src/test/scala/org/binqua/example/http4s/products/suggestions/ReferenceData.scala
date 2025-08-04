package org.binqua.example.http4s.products.suggestions

import cats.data.NonEmptyList

object ReferenceData {

  val productId1: ProductId = ProductId("1")

  val productId2: ProductId = ProductId("2")

  val product1: Product = Product(productId1, description = ProductDesc("t1"), tags = NonEmptyList.of(Tag("A")))

  val product2: Product = ReferenceData.product1.copy(id = ReferenceData.productId2, tags = NonEmptyList.of(Tag("A")))

  val productsWithProduct1AndProduct2: Products = Products.unsafeFrom(List(product1, product2))
}
