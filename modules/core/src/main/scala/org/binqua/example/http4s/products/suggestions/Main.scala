package org.binqua.example.http4s.products.suggestions

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = Http4sProductsSuggestionsServer.run[IO]()
}
