package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.CA21engineer.HouseHackathonUnityServer.grpc.room._
import akka.grpc.scaladsl.Metadata

import scala.concurrent.Future
import scala.concurrent.duration._

// Mock
class RoomServicePowerApiImpl(implicit materializer: Materializer) extends RoomServicePowerApi {

  override def createRoom(in: CreateRoomRequest, metadata: Metadata): Source[RoomResponse, NotUsed] = {
    Source(
      List(
        RoomResponse(
          RoomResponse.Response.CreateRoomResponse(
            CreateRoomResponse("roomId")
          )
        ),
        RoomResponse(
          RoomResponse.Response.JoinRoomResponse(
            JoinRoomResponse("roomId")
          )
        ),
        RoomResponse(
          RoomResponse.Response.ReadyResponse(
            ReadyResponse(Seq.empty, Seq(Member("user1"), Member("user2"), Member("user3"), Member("user4")), Direction.Up, java.time.Instant.now().toString)
          )
        )
      )
    ).throttle(1, 2.seconds)
  }

  override def joinRoom(in: JoinRoomRequest, metadata: Metadata): Source[RoomResponse, NotUsed] = {
    Source(
      List(
        RoomResponse(
          RoomResponse.Response.CreateRoomResponse(
            CreateRoomResponse("roomId")
          )
        ),
        RoomResponse(
          RoomResponse.Response.JoinRoomResponse(
            JoinRoomResponse("roomId")
          )
        ),
        RoomResponse(
          RoomResponse.Response.ReadyResponse(
            ReadyResponse(Seq.empty, Seq(Member("user1"), Member("user2"), Member("user3"), Member("user4")), Direction.Up, java.time.Instant.now().toString)
          )
        )
      )
    ).throttle(1, 2.seconds)
  }

  override def coordinateSharing(in: Source[Coordinate, NotUsed], metadata: Metadata): Source[Coordinate, NotUsed] = {
    (metadata.getText("roomid"), metadata.getText("accountid")) match {
      case (Some(_), Some(_)) =>
        in
      case (l, r) =>
        Source.empty
    }
  }

  override def childOperation(in: Source[Operation, NotUsed], metadata: Metadata): Future[Empty] = {
    (metadata.getText("roomid"), metadata.getText("accountid")) match {
      case (Some(_), Some(_)) =>
        in.runWith(Sink.foreach(println)).map(_ => Empty())(materializer.executionContext)
      case _ =>
        Future.failed(new Exception("MetaDataが正しくありません！: require MetaData: string RoomId, string AccountId"))
    }
  }

  override def parentOperation(in: ParentOperationRequest, metadata: Metadata): Source[Operation, NotUsed] = {
    Source.repeat(Operation(Direction.Up, 0.1f)).throttle(1, 2.seconds)
  }

}
