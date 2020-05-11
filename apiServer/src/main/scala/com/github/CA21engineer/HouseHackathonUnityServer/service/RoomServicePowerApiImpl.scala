//package com.github.CA21engineer.HouseHackathonUnityServer.service
//
//import akka.NotUsed
//import akka.stream.Materializer
//import akka.stream.scaladsl.Source
//import com.github.CA21engineer.HouseHackathonUnityServer.grpc.room._
//import akka.grpc.scaladsl.Metadata
//
//import scala.concurrent.Future
//import com.github.CA21engineer.HouseHackathonUnityServer.repository.CoordinateRepository
//import org.slf4j.{Logger, LoggerFactory}
//
//class RoomServicePowerApiImpl(roomAggregates: RoomAggregates[RoomResponse, CoordinateRecord, Operation])(implicit materializer: Materializer) extends RoomServicePowerApi {
//  val logger: Logger = LoggerFactory.getLogger(getClass)
//
//  override def createRoom(in: CreateRoomRequest, metadata: Metadata): Source[RoomResponse, NotUsed] = {
//    val roomKey = if (in.roomKey.nonEmpty) Some(in.roomKey) else None
//    logger.info(s"CreateRoomRequest: accountId = ${in.accountId}, accountName = ${in.accountName}, roomKey = $roomKey")
//    roomAggregates.createRoom(in.accountId, in.accountName, roomKey)
//  }
//
//  override def joinRoom(in: JoinRoomRequest, metadata: Metadata): Source[RoomResponse, NotUsed] = {
//    val roomKey = if (in.roomKey.nonEmpty) Some(in.roomKey) else None
//    logger.info(s"JoinRoomRequest: accountId = ${in.accountId}, accountName = ${in.accountName}, roomKey = $roomKey")
//    roomAggregates
//      .joinRoom(in.accountId, in.accountName, roomKey)
//      .getOrElse({
//        logger.error("JoinRoomRequest: failed")
//        Source.single(RoomResponse(RoomResponse.Response.Error(ErrorType.ROOM_NOT_FOUND_ERROR)))
//      })
//  }
//
//  override def coordinateSharing(in: Source[CoordinateRecord, NotUsed], metadata: Metadata): Source[CoordinateRecord, NotUsed] = {
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
//}
