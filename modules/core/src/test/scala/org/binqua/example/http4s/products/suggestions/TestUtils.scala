package org.binqua.example.http4s.products.suggestions

import cats.effect.IO

object TestUtils {
  def thisCodeShouldNotBeExercised(details: String = ""): IO[Nothing] =
    IO.raiseError(
      new IllegalStateException(
        s"this code should not be exercised.${if (details.isEmpty) "" else s"Details:$details"}"
      )
    )
}
