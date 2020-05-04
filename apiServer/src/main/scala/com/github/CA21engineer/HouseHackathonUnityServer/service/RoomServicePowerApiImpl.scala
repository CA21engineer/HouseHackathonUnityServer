package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.actor.Status
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.CA21engineer.HouseHackathonUnityServer.grpc.room._
import akka.grpc.scaladsl.Metadata

import scala.concurrent.Future

class RoomServicePowerApiImpl(implicit materializer: Materializer) extends RoomServicePowerApi {
  val roomAggregates = new RoomAggregates[RoomResponse, Coordinate, Operation]()

  override def createRoom(in: CreateRoomRequest, metadata: Metadata): Source[RoomResponse, NotUsed] = {
    roomAggregates.createRoom(in.accountId, if (in.roomKey.nonEmpty) Some(in.roomKey) else None)
  }

  override def joinRoom(in: JoinRoomRequest, metadata: Metadata): Source[RoomResponse, NotUsed] = {
    roomAggregates
      .joinRoom(in.accountId, if (in.roomKey.nonEmpty) Some(in.roomKey) else None)
      .getOrElse(Source.empty)
  }

  override def coordinateSharing(in: Source[Coordinate, NotUsed], metadata: Metadata): Source[Coordinate, NotUsed] = {
    (metadata.getText("roomid"), metadata.getText("accountid")) match {
      case (Some(roomId), Some(accountId)) =>
        roomAggregates
          .getRoomAggregate(roomId, accountId)
          .map(_.roomRef.playingDataSharingActorRef)
          .map { ref =>
            in to Sink.actorRef(ref._1, Status.Success) run()
            ref._2
          }
          .getOrElse(Source.empty)
      case _ =>
        Source.empty
    }
  }

  override def childOperation(in: Source[Operation, NotUsed], metadata: Metadata): Future[Empty] = {
    (metadata.getText("roomid"), metadata.getText("accountid")) match {
      case (Some(roomId), Some(accountId)) =>
        roomAggregates
          .getRoomAggregate(roomId, accountId)
          .map(_.roomRef.operationSharingActorRef._1)
          .map { ref =>
            in to Sink.actorRef(ref, Status.Success) run()
            Future.successful(Empty())
          }
          .getOrElse(Future.failed(new Exception("Internal error")))
      case _ =>
        Future.failed(new Exception("MetaDataが正しくありません！: require MetaData: string RoomId, string AccountId"))
    }
  }

  override def parentOperation(in: ParentOperationRequest, metadata: Metadata): Source[Operation, NotUsed] = {
    roomAggregates
      .getRoomAggregate(in.roomId, in.accountId)
      .map(_.roomRef.operationSharingActorRef._2)
      .getOrElse(Source.empty)
  }

}
