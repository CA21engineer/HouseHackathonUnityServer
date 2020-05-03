package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.Source

case class RoomActorRef[Coordinate, Operation](playingDataSharingActorRef: (ActorRef, Source[Coordinate, NotUsed]), operationSharingActorRef: (ActorRef, Source[Operation, NotUsed]))

object RoomActorRef {

  // TODO
  def create[Coordinate, Operation](): RoomActorRef[Coordinate, Operation] = ???

}

