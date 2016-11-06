import java.io.File

import sbt._
import sbt.Keys._

object ExampleBase extends Build {

  def module(moduleName: String, settings: Seq[Setting[_]] = Nil, dir: String = "") =
    Project(id = moduleName, base = file(if (dir.nonEmpty) dir else moduleName))
      .settings(name := moduleName)
      .settings(CommonSettings.buildSettings: _*)
      .settings(settings: _*)


  lazy val backendCluster =
    module("cluster-1", dir = "cluster-1", settings = ExampleCluster.settings).
      aggregate(exampleCore).
      dependsOn(exampleCore)

  lazy val exampleCluster2 =
    module("cluster-2", dir = "cluster-2", settings = ExampleCluster.settings).
      aggregate(exampleCore).
      dependsOn(exampleCore)

  lazy val exampleCore = module(
    moduleName = "core",
    dir = "modules/core",
    settings = ExampleCore.buildSettings)


}

object ExampleCluster {
  import Dependencies._

  lazy val moduleDependencies = Seq(
    akka, akkaCluster, akkaContrib
  ) ++ spraySeq ++ logging //++ testDependencies


  lazy val settings = Seq(
    version := "1.0",
    libraryDependencies ++= moduleDependencies
  )
}

object ExampleHierarchy {
  import Dependencies._

  lazy val moduleDependencies = Seq(
    akka
  ) ++ logging //++ testDependencies


  lazy val settings = Seq(
    version := "1.0",
    libraryDependencies ++= moduleDependencies
  )
}

object ExampleCore {
  import Dependencies._

  lazy val moduleDependencies = Seq(
    typesafeConfig, scalaCompiler
  ) ++ logging //++ testDependencies


  lazy val buildSettings = Seq(
    libraryDependencies ++= moduleDependencies
  )
}

object Versions {
  val akka = "2.3.16"
  val scala = "2.11.6"
  val spray = "1.3.2"
}

object Dependencies {
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.2.1"
  lazy val scalaCompiler = "org.scala-lang" % "scala-compiler" % Versions.scala
  lazy val scala = "org.scala-lang" % "scala-library" % Versions.scala

  lazy val akka = "com.typesafe.akka" %% "akka-actor" % Versions.akka
  lazy val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % Versions.akka
  lazy val akkaPersistence = "com.typesafe.akka" %% "akka-persistence-experimental" % Versions.akka
  lazy val akkaContrib = "com.typesafe.akka" %% "akka-contrib" % Versions.akka

  lazy val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % "1.7.13"
  lazy val julToSlf4j = "org.slf4j" % "jul-to-slf4j" % "1.7.13"
  lazy val jcl99empty = "commons-logging" % "commons-logging" % "99-empty"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.1.3"
  lazy val logbackCore = "ch.qos.logback" % "logback-core" % "1.1.3"
  lazy val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Versions.akka
  lazy val log4s = "org.log4s" %% "log4s" % "1.3.0"
  lazy val logging = Seq(logback, logbackCore, jclOverSlf4j, julToSlf4j, jcl99empty, akkaSlf4j, log4s)

  lazy val leveldbApi = "org.iq80.leveldb" % "leveldb-api" % "0.9"
  lazy val leveldb = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
  lazy val leveldbOSX = "org.fusesource.leveldbjni" % "leveldbjni-osx" % "1.8"

  lazy val spray = "io.spray" %% "spray-routing" % Versions.spray
  lazy val sprayClient = "io.spray" %% "spray-client" % Versions.spray

  val leveldbSeq = Seq(leveldb, leveldbOSX, leveldbApi)

  val spraySeq = Seq(spray, sprayClient)
}

object CommonSettings {
  import Dependencies._

  lazy val overrides = Set(akka, scalaCompiler, scala) ++ leveldbSeq ++ logging

  lazy val excludes = Seq(
  )
  lazy val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    scalaVersion := Versions.scala,
    organization := "com.example",
    resolvers    :=
      Seq(SonatypeSnapshots,
        Sonatype,
        Typesafe,
        TypesafeMaven,
        Version99EmptyLoggers),
    autoScalaLibrary := false,
    dependencyOverrides ++= overrides,
    excludeDependencies ++= excludes,
    fork := true

  )

  lazy val SonatypeSnapshots = "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
  lazy val Sonatype = Resolver.sonatypeRepo("releases")
  lazy val Typesafe = "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
  lazy val TypesafeMaven = "Typesafe repository mwn" at "http://repo.typesafe.com/typesafe/maven-releases/"
  lazy val Version99EmptyLoggers = "version99 Empty loggers" at "http://version99.qos.ch"
}
