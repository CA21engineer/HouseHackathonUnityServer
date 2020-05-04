package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.util.{Failure, Success, Try}

class RoomAggregates[T, Coordinate, Operation](implicit materializer: Materializer) {
  val rooms: scala.collection.mutable.Map[String, RoomAggregate[T, Coordinate, Operation]] = scala.collection.mutable.Map.empty

  def generateRoomId(): String =
    java.util.UUID.randomUUID.toString.replaceAll("-", "")

  /** ルーム作成の時に呼ばれる
   *
   *  actorRef
   *
   *  @param authorAccountId ルーム作成者ID
   *  @param roomKey ルームの合言葉: Some -> プラーベートなルーム, None -> パブリックなルーム
   *  @return
   */
  def createRoom(authorAccountId: String, roomKey: Option[String]): Source[T, NotUsed] = {
    val (roomAggregate, source) = RoomAggregate.create[T, Coordinate, Operation](authorAccountId, roomKey)
    rooms(generateRoomId()) = roomAggregate
    source
  }

  def searchVacantRoom(roomKey: Option[String]): Try[(String, RoomAggregate[T, Coordinate, Operation])] = {
    this.rooms.find(_._2.canParticipate(roomKey))
      .map(Success(_)).getOrElse(Failure(new Exception("参加可能な部屋がありません")))
  }

  def getRoomAggregate(roomId: String, accountId: String): Option[RoomAggregate[T, Coordinate, Operation]] = {
    this.rooms
      .get(roomId)
      .filter(a => a.children.exists(_._1 == accountId))
  }

  def joinRoom(accountId: String, roomKey: Option[String]): Try[Source[T, NotUsed]] =
    for {
      (roomId, roomAggregate) <- this.searchVacantRoom(roomKey)
      newRoomAggregate <- roomAggregate.joinRoom(accountId, roomKey)
    } yield {
      if (newRoomAggregate._1.isFull) {
        //TODO 準備完了通知
        newRoomAggregate._1.parent._2 ! ""
        newRoomAggregate._1.children.foreach(_._2 ! "")
      }
      this.rooms.update(roomId, newRoomAggregate._1)
      newRoomAggregate._2
    }

}

