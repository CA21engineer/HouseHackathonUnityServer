package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream._
import akka.stream.scaladsl._

case class RoomActorRef[Coordinate, Operation](playingDataSharingActorRef: (ActorRef, Source[Coordinate, NotUsed]), operationSharingActorRef: (ActorRef, Source[Operation, NotUsed]))

object RoomActorRef {

  // TODO
  def create[Coordinate, Operation](killSwitch: SharedKillSwitch)(implicit materializer: Materializer): RoomActorRef[Coordinate, Operation] = {
    RoomActorRef[Coordinate, Operation](
      playingDataSharingActorRef = createSource(killSwitch),
      operationSharingActorRef = createSource(killSwitch)
    )
  }

  def createSource[T](killSwitch: SharedKillSwitch)(implicit materializer: Materializer): (ActorRef, Source[T, NotUsed]) = {
    val actorRefSource: Source[T, ActorRef] = Source.actorRef[T](bufferSize = 1000, OverflowStrategy.fail) via killSwitch.flow
    actorRefSource.toMat(BroadcastHub.sink[T](bufferSize = 256))(Keep.both).run()
  }

}

