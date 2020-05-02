import sbt._

object Akka {
  private val version     = "2.5.19"
  val actor: ModuleID     = "com.typesafe.akka" %% "akka-actor" % version
  val stream: ModuleID    = "com.typesafe.akka" %% "akka-stream" % version
  val persistence: ModuleID = "com.typesafe.akka" %% "akka-persistence" % version
  val `persistence-query`: ModuleID = "com.typesafe.akka" %% "akka-persistence-query" % version
  val cluster: ModuleID = "com.typesafe.akka" %% "akka-cluster" % version
  val clusterTools: ModuleID = "com.typesafe.akka" %% "akka-cluster-tools" % version
  val clusterSharding: ModuleID = "com.typesafe.akka" %% "akka-cluster-sharding" % version
  val testkit: ModuleID   = "com.typesafe.akka" %% "akka-testkit" % version
  val slf4j: ModuleID     = "com.typesafe.akka" %% "akka-slf4j" % version
  val contrib: ModuleID     = "com.typesafe.akka" %% "akka-contrib" % version
  val experimental: ModuleID = "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.4.11.2"
  private val httpVersion = "10.1.7"
  val http                = "com.typesafe.akka" %% "akka-http" % httpVersion
  val httpTestKit         = "com.typesafe.akka" %% "akka-http-testkit" % httpVersion

  val `akka-http-crice` = "de.heikoseeberger" %% "akka-http-circe" % "1.24.3"
}

object Circe {
  private val version   = "0.11.1"
  val core: ModuleID    = "io.circe" %% "circe-core" % version
  val parser: ModuleID  = "io.circe" %% "circe-parser" % version
  val generic: ModuleID = "io.circe" %% "circe-generic" % version
  val extras: ModuleID  = "io.circe" %% "circe-generic-extras" % version
  val shapes: ModuleID = "io.circe" %% "circe-shapes" % version
}

object Logback {
  private val version   = "1.2.3"
  val classic: ModuleID = "ch.qos.logback" % "logback-classic" % version
}

object LogstashLogbackEncoder {
  private val version = "4.11"
  val encoder = "net.logstash.logback" % "logstash-logback-encoder" % version excludeAll (
    ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-core"),
    ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-databind")
  )
}

object ScalaLogging {
  private val version      = "3.5.0"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % version
}

object Config {
  private val version = "1.3.4"
  val core = "com.typesafe" % "config" % version
}

object Monix {
  val monixVersion = "3.0.0-RC2"
  val version      = "io.monix" %% "monix" % monixVersion
}

object MySQLConnectorJava {
  val version = "mysql" % "mysql-connector-java" % "5.1.42"
}

object Redis {
  val client   = "com.github.etaty" %% "rediscala"     % "1.8.0"
  val embRedis = "com.chatwork"     % "embedded-redis" % "0.7"
}

object Aerospike {
  private val version = "1.1.14"

  val core = "ru.tinkoff" %% "aerospike-scala" % version
  val client = "com.aerospike" % "aerospike-client" % "3.3.1" // in case you don't have it
  val example = "ru.tinkoff" %% "aerospike-scala-example" % version // usage examples
  val proto = "ru.tinkoff" %% "aerospike-scala-proto" % version // protobuff serialization support

  val all = Seq(core, client, example, proto)
}

object `doobie-quill` {
  val all =
    Seq(
      "org.typelevel" %% "cats-core" % "2.0.0",
      "com.chuusai" %% "shapeless" % "2.3.3",
      "org.tpolecat" %% "doobie-core"      % "0.8.4"
    ) ++
      ("-hikari" :: "-quill" :: Nil).map(a => "org.tpolecat" %% s"doobie$a" % "0.8.6")
}

object AkkaServerSupport {
  val resolver = "Maven Repo on github" at "https://BambooTuna.github.io/AkkaServerSupport"

  private val version = "1.1.2-SNAPSHOT"
  val core = "com.github.BambooTuna" %% "akkaserversupport-core" % version
  val authentication = "com.github.BambooTuna" %% "akkaserversupport-authentication" % version
  val cooperation = "com.github.BambooTuna" %% "akkaserversupport-cooperation" % version
}
