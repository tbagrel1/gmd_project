name := "gmd_project"

version := "0.1"

scalaVersion := "2.13.2"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.49"
libraryDependencies += "com.outr" %% "lucene4s" % "1.9.1"
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.8.10.1"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.6"
libraryDependencies += "io.dylemma" %% "xml-spac" % "0.8"
libraryDependencies += "io.dylemma" %% "json-spac" % "0.8"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
libraryDependencies += "org.scala-graph" %% "graph-core" % "1.13.2"
libraryDependencies += "org.scala-graph" %% "graph-dot" % "1.13.0"
