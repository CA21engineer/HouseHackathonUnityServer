package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.actor.Status
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}

import scala.util.{Failure, Success, Try}
import com.github.CA21engineer.HouseHackathonUnityServer.repository

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
    val roomId = generateRoomId()
    rooms(roomId) = roomAggregate
    source// via KillSwitches.shared(roomId).flow
  }

  def searchVacantRoom(roomKey: Option[String]): Try[(String, RoomAggregate[T, Coordinate, Operation])] = {
    this.rooms.find(_._2.canParticipate(roomKey))
      .map(Success(_)).getOrElse(Failure(new Exception("参加可能な部屋がありません")))
  }

  def getRoomAggregate(roomId: String, accountId: String): Option[RoomAggregate[T, Coordinate, Operation]] = {
    this.rooms
      .get(roomId)
      .filter(a => a.parent._1 == accountId || a.children.exists(_._1 == accountId))
  }

  def joinRoom(accountId: String, roomKey: Option[String]): Try[Source[T, NotUsed]] =
    for {
      (roomId, roomAggregate) <- this.searchVacantRoom(roomKey)
      (newRoomAggregate, source) <- roomAggregate.joinRoom(accountId, roomKey)
    } yield {
      import com.github.CA21engineer.HouseHackathonUnityServer.grpc.room._
      val allMember = newRoomAggregate.children + newRoomAggregate.parent
      if (newRoomAggregate.isFull) {
        // 操作方向の抽選
        val directions: Seq[Direction] = scala.util.Random.shuffle(List(Direction.Up,Direction.Down,Direction.Left,Direction.Right))
        
        val ghostRec = repository.CoordinateRepository.findBestRecord()
        val readyResponse = { direction: Direction =>
          RoomResponse(RoomResponse.Response.ReadyResponse(ReadyResponse(
            roomId = roomId,
            ghostRecord = ghostRec,
            member = allMember.map(a => Member(a._1)).toSeq,
            direction = direction,
            date = java.time.Instant.now().toString
          )))
        }

        // 準備完了通知
        allMember
          .zip(directions)
          .foreach { a =>
            println(s"Ready通知: ${a._1._1}")
            Source(List(readyResponse(a._2))) to Sink.actorRef(a._1._2, Status.Success) run()
          }
        repository.RoomRepository.create(roomId) // insert db
      }
      this.rooms.update(roomId, newRoomAggregate)
      //TODO 参加完了通知: 後何人
      allMember.foreach { a =>
        a._2 ! RoomResponse(RoomResponse.Response.JoinRoomResponse(JoinRoomResponse(
          roomId = roomId,
          vagrant = newRoomAggregate.vacantPeople
        )))
      }

      source
    }

}

