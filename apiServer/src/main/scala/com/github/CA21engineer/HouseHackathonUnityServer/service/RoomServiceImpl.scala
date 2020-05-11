package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source}
import com.github.CA21engineer.HouseHackathonUnityServer.model.{ChildData, Coordinate, CoordinateRecord, CreateRoomRequest, ErrorResponse, JoinRoomRequest, JoinRoomResponse, MalformedMessageType, Operation, ParentData, ReadyResponse, RoomNotFound, WebsocketData}
import org.slf4j.{Logger, LoggerFactory}
import shapeless.{:+:, CNil, Coproduct}
import io.circe.generic.auto._
import io.circe.shapes._
import io.circe.parser

import scala.util.{Failure, Success, Try}


class RoomServiceImpl(roomAggregates: RoomAggregates)(implicit materializer: Materializer) {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  type Event = Coordinate :+: Operation :+: CNil

  def createRoom(in: CreateRoomRequest, inSource: Source[WebsocketData, NotUsed]): (String, Source[WebsocketData, NotUsed]) = {
    logger.info(s"CreateRoomRequest: accountId = ${in.accountId}, accountName = ${in.accountName}, roomKey = ${in.roomKey}")
    val (roomId, outSource) = roomAggregates.createRoom(in.accountId, in.accountName, in.roomKey)
    roomAggregates
      .getRoomAggregate(roomId)
        .foreach { roomAggregate =>
          inSource.map {
            case v: Coordinate =>
              roomAggregate.children.foreach(_.actorRef ! v)
            case v: ErrorResponse =>
              roomAggregate.parent.actorRef ! v
          }
        }
    (roomId, outSource)
  }

  def joinRoom(in: JoinRoomRequest, inSource: Source[WebsocketData, NotUsed]): Try[(String, Source[WebsocketData, NotUsed])] = {
    logger.info(s"JoinRoomRequest: accountId = ${in.accountId}, accountName = ${in.accountName}, roomKey = ${in.roomKey}")
    roomAggregates
      .joinRoom(in.accountId, in.accountName, in.roomKey)
      .map { a =>
        roomAggregates
          .getRoomAggregate(a._1)
          .foreach { roomAggregate =>
            inSource.map {
              case v: Operation =>
                roomAggregate.parent.actorRef ! v
              case v: ErrorResponse =>
                roomAggregate.parent.actorRef ! v
            }
          }
        a
      }
  }


  def parentFlow(in: CreateRoomRequest): Flow[Message, Message, NotUsed] = {
    val actorRefSource: Source[WebsocketData, ActorRef] = Source.actorRef[WebsocketData](bufferSize = 1000, OverflowStrategy.fail)
    val (actorRef, inSource) = actorRefSource.toMat(BroadcastHub.sink[WebsocketData](bufferSize = 256))(Keep.both).run()
    val inDecode = Flow[Message].collect {
      case TextMessage.Strict(message) =>
        logger.info(s"ReceiveParentMessage: $message")
        parser.decode[Event](message) match {
          case Right(v) =>
            logger.info(s"ReceiveParentMessage: Right#$v")
            Coproduct.unsafeGet(v).asInstanceOf[WebsocketData]
          case Left(e) =>
            logger.info(s"ReceiveParentMessage: Right#$e")
            ErrorResponse(MalformedMessageType, e.getMessage)
        }
    }
    val outDecode = Flow[WebsocketData].map { s =>
      logger.info(s"Send to client: ${s.toString}")
      TextMessage(s.toString)
    }
    val (roomId, outSource) =  createRoom(in, inSource)
    val flow = inDecode via Flow.fromSinkAndSource[WebsocketData, WebsocketData](Sink.actorRef(actorRef, "Complete"), outSource) via outDecode
    roomAggregates.watchParentSource(flow, roomId)
  }

  def childFlow(in: JoinRoomRequest): Flow[Message, Message, NotUsed] = {
    val actorRefSource: Source[WebsocketData, ActorRef] = Source.actorRef[WebsocketData](bufferSize = 1000, OverflowStrategy.fail)
    val (actorRef, inSource) = actorRefSource.toMat(BroadcastHub.sink[WebsocketData](bufferSize = 256))(Keep.both).run()
    val inDecode = Flow[Message].collect {
      case TextMessage.Strict(message) =>
        logger.info(s"ReceiveChildMessage: $message")
        parser.decode[Event](message) match {
          case Right(v) =>
            logger.info(s"ReceiveChildMessage: Right#$v")
            Coproduct.unsafeGet(v).asInstanceOf[WebsocketData]
          case Left(e) =>
            logger.info(s"ReceiveChildMessage: Right#$e")
            ErrorResponse(MalformedMessageType, e.getMessage)
        }
    }
    val outDecode = Flow[WebsocketData].map { s =>
      logger.info(s"Send to client: ${s.toString}")
      TextMessage(s.toString)
    }
    joinRoom(in, inSource) match {
      case Failure(_) =>
        inDecode via Flow.fromSinkAndSource[WebsocketData, WebsocketData](Sink.ignore, Source.empty) via outDecode
      case Success((roomId, outSource)) =>
        val flow = inDecode via Flow.fromSinkAndSource[WebsocketData, WebsocketData](Sink.actorRef(actorRef, "Complete"), outSource) via outDecode
        roomAggregates.watchLeavingRoomSource(flow, roomId, in.accountId)
    }
  }

//  override def coordinateSharing(in: Source[CoordinateRecord, NotUsed], metadata: Metadata): Source[WebsocketData, NotUsed] = {
//    logger.info(s"CoordinateSharingRequest")
//    (metadata.getText("roomid"), metadata.getText("accountid")) match {
//      case (Some(roomId), Some(accountId)) =>
//        logger.info(s"CoordinateSharingRequest: roomId = $roomId, accountId = $accountId")
//        roomAggregates
//          .getRoomAggregate(roomId, accountId)
//          .map(_.roomRef.playingDataSharingActorRef)
//          .map { ref =>
//            in.runForeach(a => ref._1 ! a)
//            ref._2
//          }
//          .getOrElse({
//            logger.error("CoordinateSharingRequest: failed")
//            Source.empty
//          })
//      case _ =>
//        logger.error(s"CoordinateSharingRequest: meta failed, roomId = ${metadata.getText("roomid")}, accountId = ${metadata.getText("accountid")}")
//        Source.empty
//    }
//  }
//
//  override def childOperation(in: Source[Operation, NotUsed], metadata: Metadata): Source[Empty, NotUsed] = {
//    logger.info(s"ChildOperationRequest")
//    (metadata.getText("roomid"), metadata.getText("accountid")) match {
//      case (Some(roomId), Some(accountId)) =>
//        logger.info(s"ChildOperationRequest: roomId = $roomId, accountId = $accountId")
//        roomAggregates
//          .getRoomAggregate(roomId, accountId)
//          .map(_.roomRef.operationSharingActorRef._1)
//          .map { ref =>
//            in.map(a => {
//              logger.debug(s"ChildOperationRequest: $a")
//              ref ! a
//              Empty()
//            })
//          }
//          .getOrElse({
//            logger.error("ChildOperationRequest: failed")
//            Source.empty
//          })
//      case _ =>
//        logger.error(s"ChildOperationRequest: meta failed, roomId =  ${metadata.getText("roomid")}, accountId = ${metadata.getText("accountid")}")
//        Source.empty
//    }
//  }
//
//  override def parentOperation(in: ParentOperationRequest, metadata: Metadata): Source[Operation, NotUsed] = {
//    logger.info(s"ParentOperationRequest: roomId = ${in.roomId}, accountId = ${in.accountId}")
//    roomAggregates
//      .getRoomAggregate(in.roomId, in.accountId)
//      .map(_.roomRef.operationSharingActorRef._2)
//      .getOrElse({
//        logger.error("ParentOperationRequest: failed")
//        Source.empty
//      })
//  }
//
//  override def sendResult(in: SendResultRequest, metadata: Metadata): Future[Empty] = {
//    // 親のみ書き込み可能
//    logger.info(s"SendResultRequest: ${in.roomId}, ${in.accountId}, ${in.ghostRecord}")
//    roomAggregates
//      .getRoomAggregate(in.roomId, in.accountId)
//      .filter(_.parent._1 == in.accountId)
//      .map { aggregate =>
//        logger.info(s"SendResultRequest: roomId = ${in.roomId}, accountId = ${in.accountId}, ghostRecordSize = ${in.ghostRecord.size}, isGameClear = ${in.isGameClear}, clearTime = ${in.date}")
//        val start = java.time.Instant.now().toEpochMilli
//        Future { CoordinateRepository.recordData(100, in.roomId, in.ghostRecord) }(materializer.executionContext)
//          .onComplete(_ => logger.info(s"CoordinateRepository: processing time = ${java.time.Instant.now().toEpochMilli - start}"))(materializer.executionContext)
//        aggregate.children.foreach(_._3 ! RoomResponse(RoomResponse.Response.Result(SimpleGameResult(in.isGameClear, in.date))))
//      }
//      .fold({
//        logger.info("SendResultRequest: failed")
//        Future.failed[Empty](new Exception("Internal Error!!!"))
//      })(_ => Future.successful(Empty()))
//  }
}
