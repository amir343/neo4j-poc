name := "mysql-to-neo4j-migration"

version := "1.0"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
    "com.typesafe" %% "slick" % "1.0.0-RC1",
    "mysql" % "mysql-connector-java" % "5.1.22"
    )