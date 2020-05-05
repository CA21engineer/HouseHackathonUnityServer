package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.github.CA21engineer.HouseHackathonUnityServer.grpc.room._
import akka.grpc.scaladsl.Metadata

import scala.concurrent.Future

import com.github.CA21engineer.HouseHackathonUnityServer.repository.CoordinateRepository

class RoomServicePowerApiImpl(implicit materializer: Materializer) extends RoomServicePowerApi {
  val roomAggregates = new RoomAggregates[RoomResponse, Coordinate, Operation]()

  override def createRoom(in: CreateRoomRequest, metadata: Metadata): Source[RoomResponse, NotUsed] = {
    println(s"createRoom: ${in.accountId}, ${in.accountName}, ${in.roomKey}")
    roomAggregates.createRoom(in.accountId, in.accountName, if (in.roomKey.nonEmpty) Some(in.roomKey) else None)
  }

  override def joinRoom(in: JoinRoomRequest, metadata: Metadata): Source[RoomResponse, NotUsed] = {
    println(s"joinRoom: ${in.accountId}, ${in.accountName}, ${in.roomKey}")
    roomAggregates
      .joinRoom(in.accountId, in.accountName, if (in.roomKey.nonEmpty) Some(in.roomKey) else None)
      .getOrElse({
        println("joinRoom not found")
        Source.empty
      })
  }

  override def coordinateSharing(in: Source[Coordinate, NotUsed], metadata: Metadata): Source[Coordinate, NotUsed] = {
    (metadata.getText("roomid"), metadata.getText("accountid")) match {
      case (Some(roomId), Some(accountId)) =>
        println(s"coordinateSharing: $roomId, $accountId")
        roomAggregates
          .getRoomAggregate(roomId, accountId)
          .map(_.roomRef.playingDataSharingActorRef)
          .map { ref =>
            in.runForeach(a => ref._1 ! a)
            ref._2
          }
          .getOrElse({
            println("coordinateSharing not found")
            Source.empty
          })
      case _ =>
        println(s"coordinateSharing: meta failed: ${metadata.getText("roomid")}, ${metadata.getText("accountid")}")
        Source.empty
    }
  }

  override def childOperation(in: Source[Operation, NotUsed], metadata: Metadata): Source[Empty, NotUsed] = {
    (metadata.getText("roomid"), metadata.getText("accountid")) match {
      case (Some(roomId), Some(accountId)) =>
        println(s"childOperation: $roomId, $accountId")
        roomAggregates
          .getRoomAggregate(roomId, accountId)
          .map(_.roomRef.operationSharingActorRef._1)
          .map { ref =>
            in.map(a => {
              println(s"----- childOperation: $a")
              ref ! a
              Empty()
            })
          }
          .getOrElse({
            println("childOperation not found")
            Source.empty
          })
      case _ =>
        println(s"childOperation: meta failed: ${metadata.getText("roomid")}, ${metadata.getText("accountid")}")
        Source.empty
    }
  }

  override def parentOperation(in: ParentOperationRequest, metadata: Metadata): Source[Operation, NotUsed] = {
    println(s"parentOperation: ${in.roomId}, ${in.accountId}")
    roomAggregates
      .getRoomAggregate(in.roomId, in.accountId)
      .map(_.roomRef.operationSharingActorRef._2)
      .getOrElse({
        println("parentOperation not found")
        Source.empty
      })
  }

  override def sendResult(in: SendResultRequest, metadata: Metadata): Future[Empty] = {
    // 親のみ書き込み可能
    println(s"sendResult: ${in.roomId}, ${in.accountId}, ${in.ghostRecord}")
    roomAggregates
      .getRoomAggregate(in.roomId, in.accountId)
      .filter(_.parent._1 == in.accountId)
      .map { aggregate =>
        println("CoordinateRepository create Future")
        Future {
          println("CoordinateRepository start")
//          CoordinateRepository.recordData(in.roomId, in.ghostRecord)
          println("CoordinateRepository complete")
        }(materializer.executionContext)
        println("CoordinateRepository end Future")
        aggregate.children.foreach(_._3 ! SimpleGameResult(in.isGameClear, in.date))
      }
      .fold({
        println("sendResult not found")
        Future.failed[Empty](new Exception("Internal Error!!!"))
      })(_ => Future.successful(Empty()))
  }
}
