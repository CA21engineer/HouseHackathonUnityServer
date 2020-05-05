package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.actor.Status
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}

import scala.util.{Failure, Success, Try}

class RoomAggregates[T, Coordinate, Operation](implicit materializer: Materializer) {
  val rooms: scala.collection.mutable.Map[String, RoomAggregate[T, Coordinate, Operation]] = scala.collection.mutable.Map.empty

  def watchSource[S](source: Source[S, NotUsed], roomId: String): Source[S, NotUsed] = {
    source.watchTermination()((f, d) => {
      d.foreach(_ => closeRoom(roomId))(materializer.executionContext)
      f
    })
  }

  def closeRoom(roomId: String): Unit = {
    rooms.get(roomId)
      .flatMap(_ => rooms.remove(roomId))
      .foreach(_.killSwitch.shutdown())
  }

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
    val roomId = generateRoomId()
    val (roomAggregate, source) = RoomAggregate.create[T, Coordinate, Operation](authorAccountId, roomKey, roomId)
    rooms(roomId) = roomAggregate
    watchSource(source, roomId)
  }

  def searchVacantRoom(roomKey: Option[String]): Try[(String, RoomAggregate[T, Coordinate, Operation])] = {
    this.rooms.find(_._2.canParticipate(roomKey))
      .map(Success(_)).getOrElse(Failure(new Exception("参加可能な部屋がありません")))
  }

  def getRoomAggregate(roomId: String, accountId: String): Option[RoomAggregate[T, Coordinate, Operation]] = {
    this.rooms
      .get(roomId)
      .collect {
        case roomAggregate if roomAggregate.parent._1 == accountId =>
          roomAggregate.copy(
            roomRef = roomAggregate.roomRef.copy(
              playingDataSharingActorRef = (
                roomAggregate.roomRef.playingDataSharingActorRef._1,
                watchSource(roomAggregate.roomRef.playingDataSharingActorRef._2, roomId)
              ),
              operationSharingActorRef = (
                roomAggregate.roomRef.operationSharingActorRef._1,
                watchSource(roomAggregate.roomRef.operationSharingActorRef._2, roomId)
              )
            )
          )
        case roomAggregate if roomAggregate.children.exists(_._1 == accountId) =>
          roomAggregate
      }
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

        // TODO ゴースト情報の取得
        val readyResponse = { direction: Direction =>
          RoomResponse(RoomResponse.Response.ReadyResponse(ReadyResponse(
            roomId = roomId,
            ghostRecord = Seq.empty,
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
            a._1._2 ! readyResponse(a._2)
          }
      }
      this.rooms.update(roomId, newRoomAggregate)
      allMember.foreach { a =>
        a._2 ! RoomResponse(RoomResponse.Response.JoinRoomResponse(JoinRoomResponse(
          roomId = roomId,
          vagrant = newRoomAggregate.vacantPeople
        )))
      }

      source
    }

}

