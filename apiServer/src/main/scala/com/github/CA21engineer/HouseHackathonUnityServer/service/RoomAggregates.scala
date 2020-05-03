package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.util.{Failure, Success, Try}

class RoomAggregates {
  val rooms: scala.collection.mutable.Map[String, RoomAggregate] = scala.collection.mutable.Map.empty

  def generateRoomId(): String =
    java.util.UUID.randomUUID.toString.replaceAll("-", "")

  def createRoom[T](authorAccountId: String, roomKey: Option[String])(implicit materializer: Materializer): Source[T, NotUsed] = {
    val (roomAggregate, source) = RoomAggregate.create(authorAccountId, roomKey)
    rooms(generateRoomId()) = roomAggregate
    source
  }

  def searchVacantRoom(): Try[(String, RoomAggregate)] = {
    this.rooms.find(_._2.canParticipate)
      .map(Success(_)).getOrElse(Failure(new Exception("参加可能な部屋がありません")))
  }

  def searchPrivateRoom(roomKey: String): Try[(String, RoomAggregate)] = {
    this.rooms.find(a => a._2.roomKey.contains(roomKey) && a._2.canParticipate)
      .map(Success(_)).getOrElse(Failure(new Exception("参加可能な部屋がありません")))
  }

  def joinRandomly(accountId: String): Try[String] =
    for {
      (roomId, roomAggregate) <- this.searchVacantRoom()
      newRoomAggregate <- roomAggregate.joinRoom(accountId, None)
    } yield {
      this.rooms.update(roomId, newRoomAggregate)
      roomId
    }

  def joinPrivateRoom(accountId: String, roomKey: String): Try[String] = {
    for {
      (roomId, roomAggregate) <- this.searchPrivateRoom(roomKey)
      newRoomAggregate <- roomAggregate.joinRoom(accountId, Some(roomKey))
    } yield {
      this.rooms.update(roomId, newRoomAggregate)
      roomId
    }
  }

}

