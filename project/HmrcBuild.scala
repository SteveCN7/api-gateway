import play.sbt.PlayImport._
import play.sbt.PlayScala
import sbt.Keys._
import sbt._

object HmrcBuild extends Build {

  val apiGateway = (project in file("."))
    .enablePlugins(PlayScala)
    .settings(
      scalaVersion := "2.11.8",
      name := "api-gateway",
      version := "0.1.0-SNAPSHOT",
      libraryDependencies ++= Seq(ws)
    )
}