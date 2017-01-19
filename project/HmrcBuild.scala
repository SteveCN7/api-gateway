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
import sbt.Tests.{SubProcess, Group, Filter}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning

object HmrcBuild extends Build {

  private val compileDependencies = Seq(
    ws,
    cache,
    "org.scala-lang.modules" % "scala-async_2.11" % "0.9.6",
    "uk.gov.hmrc" %% "frontend-bootstrap" % "7.10.0",
    "uk.gov.hmrc" %% "play-health" % "2.0.0",
    "uk.gov.hmrc" %% "play-url-binders" % "2.0.0",
    "uk.gov.hmrc" %% "play-config" % "3.0.0",
    "uk.gov.hmrc" %% "play-filters" % "5.6.0",
    "uk.gov.hmrc" %% "logback-json-logger" % "3.1.0",
    "com.kenshoo" %% "metrics-play" % "2.4.0_0.4.1"
  )
  private val testDependencies = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "2.2.0" % "test,it",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test,it",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % "test,it",
    "org.mockito" % "mockito-core" % "2.3.0" % "test,it",
    "com.github.tomakehurst" % "wiremock" % "2.1.12" % "test,it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0-M1" %  "test,it",
    "org.scalaj" %% "scalaj-http" % "2.3.0" % "test,it"
  )
  val appName = "api-gateway"

  val main = Project(appName, file("."))
    .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .enablePlugins(PlayScala)
    .settings(
      scalaVersion := "2.11.8",
      libraryDependencies ++= compileDependencies ++ testDependencies
    )
    .configs(IntegrationTest)
    .settings(Defaults.itSettings: _*)
    .settings(
      testOptions in Test := Seq(Filter(_ startsWith "uk.gov.hmrc")),
      testOptions in IntegrationTest := Seq(Filter(_ startsWith "it.uk.gov.hmrc")),
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "test")),
      unmanagedResourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "test/resources")),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
    )
    .settings(
      resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += Resolver.jcenterRepo
    )

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}
