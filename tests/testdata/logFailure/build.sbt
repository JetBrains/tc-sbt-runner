name := "tc-log-failure"

organization := "com.clueda"

scalaVersion := "2.10.4"

version := "0.1-SNAPSHOT"

// Scala compiler options
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

enablePlugins(PlayScala)

// public dependencies
libraryDependencies ++= Seq(
  // libraries don't matter here
)