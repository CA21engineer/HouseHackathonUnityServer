package com.github.CA21engineer.HouseHackathonUnityServer.model

//import io.circe.syntax._
//import io.circe.generic.auto._
//
//case class WebsocketData(event: String, data: String) {
//
//  override def toString: String = this.asJson.noSpaces
//
//}
//
//object WebsocketData {
//
//  def error(errorType: ErrorType): WebsocketData =
//    WebsocketData("error", errorType.message)
//}
//
//

import shapeless._


sealed trait WebsocketData
sealed trait ParentData extends WebsocketData
sealed trait ChildData extends WebsocketData


case class CreateRoomRequest(accountId: String, roomKey: Option[String], accountName: String)
case class JoinRoomRequest(accountId: String, roomKey: Option[String], accountName: String)

case class JoinRoomResponse(roomId: String, vagrant: Int) extends WebsocketData

case class ReadyResponse(roomId: String, ghostRecord: Seq[Any], member: List[Member], yourDirection: Direction) extends WebsocketData

case class Coordinate(x: Float, y: Float, z: Float, elapsedTime: Int) extends WebsocketData
case class Operation(direction: Direction, strength: Float) extends WebsocketData

case class ErrorResponse(errorType: ErrorType, message: String) extends WebsocketData




