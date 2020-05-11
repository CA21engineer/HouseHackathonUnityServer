package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}

import scala.util.{Failure, Success, Try}
import com.github.CA21engineer.HouseHackathonUnityServer.repository
import com.github.CA21engineer.HouseHackathonUnityServer.model.{Direction, ErrorResponse, JoinRoomResponse, LostConnection, Member, ReadyResponse, WebsocketData}
import org.slf4j.{Logger, LoggerFactory}

class RoomAggregates(implicit materializer: Materializer) {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  val rooms: scala.collection.mutable.Map[String, RoomAggregate] = scala.collection.mutable.Map.empty

  def watchParentSource[I, O](in: Flow[I, O, NotUsed], roomId: String): Flow[I, O, NotUsed] = {
    in.watchTermination()((f, d) => {
      d.foreach { _ =>
        logger.info("RoomAggregates: watchTermination.watchParentSource")
        this.rooms.get(roomId)
          .foreach { roomAggregate =>
            this.rooms.remove(roomId)
            sendErrorMessageToEveryOne(roomAggregate)
          }
      }(materializer.executionContext)
      f
    })
  }

  def watchLeavingRoomSource[I, O](in: Flow[I, O, NotUsed], roomId: String, accountId: String): Flow[I, O, NotUsed] = {
    in.watchTermination()((f, d) => {
      d.foreach(_ => {
        logger.info("RoomAggregates: watchTermination.watchLeavingRoomSource")
        this.rooms
          .get(roomId)
          .foreach { roomAggregate =>
            if (roomAggregate.isFull) {
              this.rooms.remove(roomId)
              sendErrorMessageToEveryOne(roomAggregate)
            } else {
              val newRoomAggregate = roomAggregate.leaveRoom(accountId)
              logger.info(s"LeavingRoom: roomId = $roomId, accountId = $accountId, vacantPeople = ${newRoomAggregate.vacantPeople}, children = ${newRoomAggregate.children}")
              this.rooms.update(roomId, newRoomAggregate)
              sendJoinResponse(roomId, newRoomAggregate)
            }
          }
      })(materializer.executionContext)
      f
    })
  }

  def sendErrorMessageToEveryOne(roomAggregate: RoomAggregate): Unit = {
    val m = roomAggregate.children + roomAggregate.parent
    m.foreach(_.actorRef ! ErrorResponse(LostConnection, "LostConnection..."))
    roomAggregate.killSwitch.shutdown()
  }

  def sendJoinResponse(roomId: String, roomAggregate: RoomAggregate): Unit = {
    val m = roomAggregate.children + roomAggregate.parent
    m.foreach(_.actorRef ! JoinRoomResponse(roomId = roomId, vagrant = roomAggregate.vacantPeople))
  }

  def sendReadyResponse(roomId: String, ghostRecord: Seq[Any], roomAggregate: RoomAggregate): Unit = {
    val directions: Seq[Direction] = Direction.shuffle
    val allMember = roomAggregate.children + roomAggregate.parent
    val allMemberHasDirection = allMember.zip(directions)
    allMemberHasDirection.foreach(a => {
      a._1.actorRef ! ReadyResponse(
        roomId = roomId,
        ghostRecord = ghostRecord,
        member = allMemberHasDirection.map(a => Member(a._1.accountName, a._2)).toList,
        direction = a._2
      )
    })
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
  def createRoom(authorAccountId: String, authorAccountName: String, roomKey: Option[String]): (String, Source[WebsocketData, NotUsed]) = {
    val roomId = generateRoomId()
    val (roomAggregate, source) = RoomAggregate.create(authorAccountId, authorAccountName, roomKey, roomId)
    rooms(roomId) = roomAggregate
    (roomId, source)
  }

  def searchVacantRoom(roomKey: Option[String]): Try[(String, RoomAggregate)] = {
    this.rooms.find(_._2.canParticipate(roomKey))
      .map(Success(_)).getOrElse(Failure(new Exception("参加可能な部屋がありません")))
  }

  def getRoomAggregate(roomId: String): Option[RoomAggregate] =
    this.rooms.get(roomId)

  def joinRoom(accountId: String, accountName: String, roomKey: Option[String]): Try[(String, Source[WebsocketData, NotUsed])] =
    for {
      (roomId, roomAggregate) <- this.searchVacantRoom(roomKey)
      (newRoomAggregate, source) <- roomAggregate.joinRoom(accountId, accountName, roomKey)
    } yield {
      if (newRoomAggregate.isFull) {
        // ゴーストレコードの取得
        val ghostRec = repository.CoordinateRepository.findBestRecord()
        // 準備完了通知
        sendReadyResponse(roomId, ghostRec, newRoomAggregate)
        repository.RoomRepository.create(roomId) // insert db
      }
      this.rooms.update(roomId, newRoomAggregate)
      sendJoinResponse(roomId, newRoomAggregate)

      (roomId, source)
    }

}

