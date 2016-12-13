import play.core.PlayVersion
import play.sbt.PlayImport._
import play.sbt.PlayScala
import sbt.Keys._
import sbt._
import sbt.Tests.Filter

object HmrcBuild extends Build {

  private val compileDependencies = Seq(
    ws,
    "uk.gov.hmrc" %% "play-config" % "3.0.0",
    "uk.gov.hmrc" %% "play-filters" % "5.6.0",
    "uk.gov.hmrc" %% "play-json-logger" % "3.0.0"
  )
  private val testDependencies = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "2.2.0" % "test,it",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test,it",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % "test,it",
    "org.mockito" % "mockito-core" % "2.3.0" % "test,it",
    "com.github.tomakehurst" % "wiremock" % "2.1.12" % "test,it",
    "org.scalaj" %% "scalaj-http" % "2.3.0" % "test,it"
  )

  val apiGateway = (project in file("."))
    .enablePlugins(PlayScala)
    .settings(
      scalaVersion := "2.11.8",
      name := "api-gateway",
      version := "0.1.0-SNAPSHOT",
      libraryDependencies ++=
        compileDependencies ++
          testDependencies
    )
    .configs(IntegrationTest)
    .settings(Defaults.itSettings: _*)
    .settings(
      testOptions in Test := Seq(Filter(_ startsWith "uk.gov.hmrc")),
      testOptions in IntegrationTest := Seq(Filter(_ startsWith "it.uk.gov.hmrc")),
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "test"))
    )
    .settings(
      resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += Resolver.jcenterRepo
    )

}
