package com.github.CA21engineer.HouseHackathonUnityServer.apiServer

import akka.actor.ActorSystem
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.http.scaladsl.UseHttp2.Always
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import com.github.CA21engineer.HouseHackathonUnityServer.service.{RoomAggregates, RoomServiceImpl}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scalikejdbc._

object Main extends App {

  val conf =
    ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())

  implicit val system: ActorSystem = ActorSystem("HouseHackathonUnityServer", conf)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  Class.forName("com.mysql.jdbc.Driver")
  ConnectionPool.singleton("jdbc:mysql://mysql:3306/unity?rewriteBatchedStatements=true", "ca21engineer", "pass")

  val roomAggregates = new RoomAggregates()

  val restServiceHandlers = new RestServiceHandler(new RoomServiceImpl(roomAggregates))
  val restBindingFuture = Http().bindAndHandle(
    handler = restServiceHandlers.toRoutes,
    interface = "0.0.0.0",
    port = 18080
  )


  sys.addShutdownHook {
    restBindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }


}
