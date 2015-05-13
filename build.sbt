name := """twitterlike"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
//  anorm,
//  cache,
//  ws,
  "com.typesafe.play" %% "play-slick" % "0.8.1",
  "mysql" % "mysql-connector-java" % "5.1.21",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "jp.t2v" %% "play2-auth"      % "0.13.2",
  "jp.t2v" %% "play2-auth-test" % "0.13.2" % "test"
)
