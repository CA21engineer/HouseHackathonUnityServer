
val sdk8 = "adoptopenjdk/openjdk8:x86_64-ubuntu-jdk8u212-b03-slim"
val scalikejdbcVersion = "2.5.2"

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
    dockerExposedPorts := Seq(18080, 18081),
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime",
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
      Akka.cors,
      Logback.classic,
      LogstashLogbackEncoder.encoder,
      Config.core,
      Monix.version,
    ),
    libraryDependencies ++= Seq(
      "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-jsr310" % scalikejdbcVersion,
      "mysql" % "mysql-connector-java" % "5.1.27"
    ),
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
  )
  .settings(
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9",
    akkaGrpcCodeGeneratorSettings := Seq("server_power_apis"),
    akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client, AkkaGrpc.Server),
    akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Scala)
  )

lazy val root =
  (project in file("."))
    .aggregate(apiServer)
    .settings(
      commands += Command.command("fullCompile") { st =>
        println("==+ clean +==")
        val st1 = Command.process("clean", st)
        println("==+ compile +==")
        val st2 = Command.process("compile", st1)
        println("==+ docker:stage +==")
        val st3 = Command.process("docker:stage", st2)
        st3
      }
    )
