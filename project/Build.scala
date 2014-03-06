import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object KafkaUtilsBuild extends Build {

  def sharedSettings = Defaults.defaultSettings ++ assemblySettings ++ Seq(
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.10.3",
    organization := "com.quantifind",
    scalacOptions := Seq("-deprecation", "-unchecked", "-optimize"),
    unmanagedJars in Compile <<= baseDirectory map { base => (base / "lib" ** "*.jar").classpath },
    retrieveManaged := true,
    transitiveClassifiers in Scope.GlobalScope := Seq("sources"),
    resolvers ++= Seq(
      "sonatype-snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      "sonatype-releases"  at "http://oss.sonatype.org/content/repositories/releases",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "JBoss Repository" at "http://repository.jboss.org/nexus/content/repositories/releases/"
    ),
    libraryDependencies ++= Seq(
      "log4j" % "log4j" % "1.2.17",
      "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      "org.apache.kafka" % "kafka_2.10" % "0.8.0.d4553d" //this should be the normal 0.8.0 release, but its not official yet ...
    ),

    // Publishing configuration

    publishMavenStyle := true,

    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      val qf = "http://repo.quantifind.com/content/repositories/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at qf + "snapshots")
      else
        Some("releases"  at qf + "releases")
    },

    publishArtifact in Test := false,

    pomIncludeRepository := { x => false }
  )

  val slf4jVersion = "1.6.1"

//Consumer project
  lazy val consumer = Project("consumer", file("consumer"), settings = consumerSettings)

  def consumerSettings = sharedSettings ++ Seq(
    name := "kafka-consumer-util",
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka_2.10" % "0.8.0.a51bd8c" //this is a version of kafka w/ commitOffsets(offsets)
    )
  )

//offsetmonitor project

  lazy val offsetmonitor = Project("offsetmonitor", file("offsetmonitor"), settings = offsetmonSettings)

  def offsetmonSettings = sharedSettings ++ Seq(
  	  name := "kafka-offset-monitor",
	  libraryDependencies ++= Seq(
	  	"net.databinder" %% "unfiltered-filter" % "0.6.7",
		"net.databinder" %% "unfiltered-jetty" % "0.6.7",
		"net.databinder" %% "unfiltered-json" % "0.6.7",
		"com.quantifind" %% "sumac" % "0.2.3",
        "com.typesafe.slick" %% "slick" % "2.0.0",
        "org.xerial" % "sqlite-jdbc" % "3.7.2",
		"com.twitter" % "util-core" % "3.0.0"
	  ),
	   resolvers ++= Seq(
	     "java m2" at "http://download.java.net/maven/2",
		 "twitter repo" at "http://maven.twttr.com"
	)
  )
}
