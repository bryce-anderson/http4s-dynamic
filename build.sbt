
organization in ThisBuild := "net.bryceanderson"
scalaVersion in ThisBuild := "2.11.7"
version in ThisBuild := "0.1.0"
licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

bintrayRepository := "http4s-bits"

// Root project

lazy val core = Project("core", new File("core"))
  .settings(libraryDependencies += providedHttp4sServer)
  .settings(projectMetadata)

lazy val example = Project("example", new File("example"))
  .settings(Revolver.settings)
  .settings(Seq(
    libraryDependencies += blazeServer,
    libraryDependencies += logbackClassic 
  ))
  .dependsOn(core)

lazy val projectMetadata = {
  val base = "github.com/bryce-anderson/http4s-dynamic"
  Seq(
    homepage := Some(url(s"https://$base")),
    startYear := Some(2015),
    licenses := Seq(
      "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
    ),
    scmInfo := {
      Some(ScmInfo(url(s"https://$base"), s"scm:git:https://$base", Some(s"scm:git:git@$base")))
    },
    pomExtra := (
      <developers>
        <developer>
          <id>brycelane</id>
          <name>Bryce L. Anderson</name>
          <email>bryce.anderson22@gmail.com</email>
        </developer>
      </developers>
      )
  )
}

val http4sVersion = "0.10.0"

lazy val providedHttp4sServer = "org.http4s" %% "http4s-server" % http4sVersion % "provided"
lazy val blazeServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.1.3"
