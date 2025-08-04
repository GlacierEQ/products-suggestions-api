import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.*

ThisBuild / scalaVersion := "2.13.14"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

val CirceVersion = "0.14.6"
val Http4sVersion = "0.23.26"
val LogbackVersion = "1.4.14"
val MunitVersion = "0.7.29"
val MunitCatsEffectVersion = "1.0.7"

lazy val root = (project in file("."))
  .settings(
    name := "products-suggestions-api"
  )
  .aggregate(core, integration)

lazy val core = (project in file("modules") / "core")
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Runtime,
      "is.cir" %% "ciris" % "3.6.0",
      "is.cir" %% "ciris-enumeratum" % "3.6.0",
      "is.cir" %% "ciris-http4s" % "3.6.0",
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion % Test
    ),
    scalacOptions ++= Seq(
      "-encoding",
      "utf-8", // Specify character encoding used by source files.
      "-explaintypes", // Explain type errors in more detail.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
      // "-Ymacro-annotations", // Enable support for macro annotations, formerly in macro paradise.

      // ********** Warning Settings ***********************************************
      "-Werror", // Fail the compilation if there are any warnings.
      "-Wdead-code", //  Warn when dead code is identified.
      "-Wextra-implicit", // Warn when more than one implicit parameter section is defined.
      // "-Wmacros:none", // Do not inspect expansions or their original trees when generating unused symbol warnings.
      // "-Wmacros:before", // Only inspect unexpanded user-written code for unused symbols. (Default)
      "-Wmacros:after", // Only inspect expanded trees when generating unused symbol warnings.
      // "-Wmacros:both", // Inspect both user-written code and expanded trees when generating unused symbol warnings.
      "-Wnumeric-widen", // Warn when numerics are widened.
      "-Woctal-literal", // Warn on obsolete octal syntax.
      // "-Wself-implicit", // Warn when an implicit resolves to an enclosing self-definition.
      "-Wunused:imports", //Warn if an import selector is not referenced.
      "-Wunused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Wunused:privates", // Warn if a private member is unused.
      "-Wunused:locals", // Warn if a local definition is unused.
      "-Wunused:explicits", // Warn if an explicit parameter is unused.
      "-Wunused:implicits", // Warn if an implicit parameter is unused.
      "-Wunused:params", // Enable -Wunused:explicits,implicits.
      "-Wvalue-discard", // Warn when non-Unit expression results are unused.

      // ********** -Xlint: Enable recommended warnings ****************************
      "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
      "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any", // Warn when a type argument is inferred to be Any.
      // "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
      "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
      "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
      "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
      "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
      "-Xlint:option-implicit", // Option.apply used implicit view.
      "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
      "-Xlint:package-object-classes", // Class or object defined in package object.
      "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
      "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:nonlocal-return", // A return statement used an exception for flow control.
      "-Xlint:implicit-not-found", // Check @implicitNotFound and @implicitAmbiguous messages.
      "-Xlint:serial", // @SerialVersionUID on traits and non-serializable classes.
      "-Xlint:valpattern", // Enable pattern checks in val definitions.
      "-Xlint:eta-zero", // Warn on eta-expansion (rather than auto-application) of zero-ary method.
      "-Xlint:eta-sam", // Warn on eta-expansion to meet a Java-defined functional interface that is not explicitly annotated with @FunctionalInterface.
      "-Xlint:deprecation" // Enable linted deprecations.
    ),
    dockerAlias := dockerAlias.value.withTag(Some("0.2")).withName("binqua-products-suggestions/web-app"),
    Compile / mainClass := Some("org.binqua.example.http4s.products.suggestions.Main"),
    Docker / packageName := "org/binqua-products-suggestions",
    dockerBaseImage := "eclipse-temurin:17.0.11_9-jre-ubi9-minimal",
    dockerExposedPorts ++= Seq(8080),
    dockerBuildCommand := {
      val platform = sys.props.getOrElse("targetPlatform", "mac") // default to mac if not set
      if (platform == "aws") {
        // Use buildx with --platform=linux/amd64 for AWS (ECS/Fargate/EC2)
        dockerExecCommand.value ++ Seq(
          "buildx", "build",
          "--platform=linux/amd64",
          "--load" // Use --push instead if pushing directly to a registry
        ) ++ dockerBuildOptions.value :+ "."
      } else {
        // Default build (native for mac, aarch64)
        dockerBuildCommand.value
      }
    }

  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val integration = (project in file("modules") / "integration")
  .dependsOn(core)
  .settings(
    name := "integration",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Runtime,
      "org.scalameta" %% "munit" % MunitVersion,
      "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.11"
    )
  )

// Automatically log system architecture when a task runs
val logSystemArch = taskKey[Unit]("Logs the current system architecture")

logSystemArch := {
  val log = streams.value.log
  val arch = sys.props("targetPlatform")
  log.info(s"Detected system architecture: $arch")
}