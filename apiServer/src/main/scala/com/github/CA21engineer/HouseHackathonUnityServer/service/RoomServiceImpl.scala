package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.github.CA21engineer.HouseHackathonUnityServer.grpc.{CreateRoomRequest, Empty, Operation, ParentOperationRequest, PlayingData, RoomResponse, RoomService}

import scala.concurrent.Future
import scala.concurrent.duration._

// Mock
class RoomServiceImpl(materializer: Materializer) extends RoomService {
  override def createRoom(in: CreateRoomRequest): Source[RoomResponse, NotUsed] = {
    println("received CreateRoomRequest: ", in.toString)
    Source(List(
      com.github.CA21engineer.HouseHackathonUnityServer.grpc.RoomResponse(
        com.github.CA21engineer.HouseHackathonUnityServer.grpc.RoomResponse.Response.CreateRoomResponse(
          com.github.CA21engineer.HouseHackathonUnityServer.grpc.CreateRoomResponse("mock_room_id")
        )
      ),
      com.github.CA21engineer.HouseHackathonUnityServer.grpc.RoomResponse(
        com.github.CA21engineer.HouseHackathonUnityServer.grpc.RoomResponse.Response.JoinRoomResponse(
          com.github.CA21engineer.HouseHackathonUnityServer.grpc.JoinRoomResponse("mock_room_id")
        )
      ),
      com.github.CA21engineer.HouseHackathonUnityServer.grpc.RoomResponse(
        com.github.CA21engineer.HouseHackathonUnityServer.grpc.RoomResponse.Response.ReadyResponse(
          com.github.CA21engineer.HouseHackathonUnityServer.grpc.ReadyResponse(
            Seq.empty,
            Seq(
              com.github.CA21engineer.HouseHackathonUnityServer.grpc.Member(in.accountId)
            ),
            com.github.CA21engineer.HouseHackathonUnityServer.grpc.Direction.Up,
            java.time.Instant.now().toString
          )
        )
      )
    )).throttle(1, 2.seconds)
  }

  override def connectPlayingData(in: Source[PlayingData, NotUsed]): Source[PlayingData, NotUsed] = {
    in.map(a => {
      println("received connectPlayingData: ", a.toString)
      a
    })
  }

  override def childOperation(in: Source[Operation, NotUsed]): Future[Empty] = {
    in.map(a => {
      println("received childOperation: ", a.toString)
      a
    })
    Future.successful(com.github.CA21engineer.HouseHackathonUnityServer.grpc.Empty())
  }

  override def parentOperation(in: ParentOperationRequest): Source[Operation, NotUsed] = {
    val r =
      com.github.CA21engineer.HouseHackathonUnityServer.grpc.Operation(
        "roomId",
        com.github.CA21engineer.HouseHackathonUnityServer.grpc.Direction.Up,
        0.1f
      )
    Source.repeat(r).throttle(1, 2.seconds)
  }
}
