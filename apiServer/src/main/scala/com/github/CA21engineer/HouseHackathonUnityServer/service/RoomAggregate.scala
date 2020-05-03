package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{BroadcastHub, Keep, Source}

import scala.util.Try

case class RoomAggregate(parent: String, children: Set[String], roomRef: ActorRef, roomKey: Option[String]) {
  private val maxCapacity = 3
  require(parent.nonEmpty)
  require(children.size <= maxCapacity, "満員です")

  def vacantPeople: Int = children.size
  def canParticipate: Boolean = vacantPeople < maxCapacity

  def joinRoom(accountId: String, roomKey: Option[String]): Try[RoomAggregate] = Try {
    this.roomKey.foreach { value =>
      require(roomKey.contains(value), "合言葉が違います")
    }
    copy(children = children + accountId)
  }

}

object RoomAggregate {
  def create[T](authorAccountId: String, roomKey: Option[String])(implicit materializer: Materializer): (RoomAggregate, Source[T, NotUsed]) = {
    val actorRefSource: Source[T, ActorRef] = Source.actorRef[T](bufferSize = 1000, OverflowStrategy.fail)
    val (actorRef, source) = actorRefSource.toMat(BroadcastHub.sink[T](bufferSize = 256))(Keep.both).run()
    (RoomAggregate(authorAccountId, Set.empty, actorRef, roomKey) ,source)
  }
}
