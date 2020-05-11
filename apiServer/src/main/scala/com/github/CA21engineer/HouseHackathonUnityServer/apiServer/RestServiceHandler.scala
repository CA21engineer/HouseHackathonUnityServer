package com.github.CA21engineer.HouseHackathonUnityServer.apiServer

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.{KillSwitches, Materializer}
import akka.stream.scaladsl.Flow
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.github.CA21engineer.HouseHackathonUnityServer.model._
import com.github.CA21engineer.HouseHackathonUnityServer.service.RoomServiceImpl
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.slf4j.{Logger, LoggerFactory}

class RestServiceHandler(roomService: RoomServiceImpl)(implicit materializer: Materializer) extends FailFastCirceSupport {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  type QueryP[Q] = Directive[Q] => Route

  val killSwitch = KillSwitches.shared("health")
  val flow = Flow[Message] via killSwitch.flow

  def toRoutes: Route = cors() {
    Router(
      route(GET, "create_room", createRoom),
      route(GET, "join_room", joinRoom),
      route(GET, "health", health)
    ).create
  }

  def health: QueryP[Unit] = _ {
    val newFlow = flow.watchTermination()((f, d) => {
      d.foreach { _ =>
        logger.info("RoomAggregates: watchTermination.watchParentSource")
        killSwitch.shutdown()
      }(materializer.executionContext)
      f
    })
    handleWebSocketMessages(newFlow)
  }

  def createRoom: QueryP[Unit] = _ {
    parameters('accountId.as[String], 'roomKey.as[String]?, 'accountName.as[String]) { (accountId, roomKey, accountName) =>
      handleWebSocketMessages(roomService.parentFlow(CreateRoomRequest(accountId, roomKey, accountName)))
    }
  }

  def joinRoom: QueryP[Unit] = _ {
    parameters('accountId.as[String], 'roomKey.as[String]?, 'accountName.as[String]) { (accountId, roomKey, accountName) =>
      handleWebSocketMessages(roomService.childFlow(JoinRoomRequest(accountId, roomKey, accountName)))
    }
  }





}
