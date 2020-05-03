
val sdk8 = "adoptopenjdk/openjdk8:x86_64-ubuntu-jdk8u212-b03-slim"

lazy val apiServer = (project in file("apiServer"))
  .enablePlugins(JavaAppPackaging, AshScriptPlugin, DockerPlugin, AkkaGrpcPlugin, JavaAgent)
  .settings(
    name := "HouseHackathonUnityServer",
    version := "0.1",
    scalaVersion := "2.12.8"
  )
  .settings(
    fork := true,
    name := "house-hackathon-uity-server",
    version := "latest",
    dockerBaseImage := sdk8,
    maintainer in Docker := "BambooTuna <bambootuna@gmail.com>",
    dockerUpdateLatest := true,
    dockerUsername := Some("bambootuna"),
    mainClass in (Compile, bashScriptDefines) := Some("com.github.CA21engineer.HouseHackathonUnityServer.apiServer.Main"),
    packageName in Docker := name.value,
    dockerExposedPorts := Seq(18080),
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.7" % "runtime"
  )
  .settings(
    libraryDependencies ++= Seq(
      ScalaTest.version     % Test,
      ScalaCheck.scalaCheck % Test,
      ScalaMock.version     % Test,
      Akka.testkit     % Test,
      Circe.core,
      Circe.generic,
      Circe.parser,
      Circe.shapes,
      Akka.http,
      Akka.stream,
      Akka.persistence,
      Akka.`persistence-query`,
      Akka.cluster,
      Akka.clusterTools,
      Akka.clusterSharding,
      Akka.slf4j,
      Akka.contrib,
      Akka.`akka-http-crice`,
      Logback.classic,
      LogstashLogbackEncoder.encoder,
      Config.core,
      Monix.version
    )
  )

lazy val root =
  (project in file("."))
    .aggregate(apiServer)
