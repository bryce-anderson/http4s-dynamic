
organization in ThisBuild := "net.bryceanderson"
scalaVersion in ThisBuild := "2.11.7"
version in ThisBuild := "0.1.0"

licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

// Root project
name := "root"
description := "Dynamic service loading for http4s"
noPublish

lazy val dynamic = Project("http4s-dynamic", file("dynamic"))
  .settings(libraryDependencies += providedHttp4sServer)
  .settings(bintraySettings)

lazy val example = Project("example", file("example"))
  .settings(noPublish)
  .settings(Revolver.settings)
  .settings(Seq(
    libraryDependencies += blazeServer,
    libraryDependencies += logbackClassic 
  ))
  .dependsOn(dynamic)

// publishing settings
lazy val bintraySettings = Seq(
  bintrayRepository := "http4s-bits",
  bintrayPackage := "http4s-dynamic",
  bintrayVcsUrl := Some("https://github.com/bryce-anderson/http4s-dynamic.git")
) ++ projectMetadata

lazy val projectMetadata = {
  val base = "github.com/bryce-anderson/http4s-dynamic"
  Seq(
    homepage := Some(url(s"https://$base")),
    startYear := Some(2015),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
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

lazy val noPublish = Seq(
  publish := (),
  bintrayRepository := "",
  bintrayPackage :="",
  publishArtifact := false
)

val http4sVersion = "0.10.0"

lazy val providedHttp4sServer = "org.http4s" %% "http4s-server" % http4sVersion % "provided"
lazy val blazeServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.1.3"
