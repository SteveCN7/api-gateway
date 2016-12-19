/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "test")),
      unmanagedResourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "test/resources"))
    )
    .settings(
      resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += Resolver.jcenterRepo
    )

}
