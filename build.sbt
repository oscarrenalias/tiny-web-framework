name := "TAWEBF"

version := "1.0"

scalaVersion := "2.9.1"

mainClass in (Compile, run) := Some("net.renalias.tawebf.core.Bootstrap")

libraryDependencies ++= Seq(
    "org.specs2" %% "specs2" % "1.6.1",
    "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test",
    "io.netty" % "netty" % "3.5.9.Final" withSources
)

libraryDependencies += "junit" % "junit" % "4.9"