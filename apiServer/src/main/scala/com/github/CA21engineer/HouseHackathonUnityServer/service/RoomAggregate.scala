package com.github.CA21engineer.HouseHackathonUnityServer.service

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{BroadcastHub, Keep, Source}

import scala.util.Try

case class RoomAggregate[T, Coordinate, Operation](parent: (String, ActorRef), children: Set[(String, ActorRef)], roomRef: RoomActorRef[Coordinate, Operation], roomKey: Option[String]) {
  // 親を除いた定員
  private val maxCapacity = 3

  require(children.size <= maxCapacity, "満員です")

  // 空き人数
  def vacantPeople: Int = maxCapacity - children.size

  def isFull: Boolean = vacantPeople == 0

  // 部屋に入れるかどうか
  def canParticipate(roomKey: Option[String]): Boolean =
    vacantPeople < maxCapacity && {
      (roomKey, this.roomKey) match {
        case (Some(a), Some(b)) => a == b
        case (_, None) => true
      }
    }

  /** ルーム参加の時に呼ばれる
   *
   *  actorRef
   *
   *  @param accountId ルームに入ろうとしているアカウントのID
   *  @param roomKey ルームの合言葉
   *  @return 満員でなく、プライベートなルームの場合は`roomKey`が一致していたら`Success[RoomAggregate]`
   */
  def joinRoom(accountId: String, roomKey: Option[String])(implicit materializer: Materializer): Try[(RoomAggregate[T, Coordinate, Operation], Source[T, NotUsed])] = Try {
    require(this.canParticipate(roomKey), "合言葉が違います")

    val (actorRef, source) = RoomAggregate.createActorRef
    val newChildren = (accountId, actorRef)
    (copy(children = this.children + newChildren), source)
  }

}

object RoomAggregate {
  def create[T, Coordinate, Operation](authorAccountId: String, roomKey: Option[String])(implicit materializer: Materializer): (RoomAggregate[T, Coordinate, Operation], Source[T, NotUsed]) = {
    val (actorRef, source) = createActorRef
    (RoomAggregate((authorAccountId, actorRef), Set.empty, RoomActorRef.create, roomKey) ,source)
  }

  def createActorRef[T](implicit materializer: Materializer): (ActorRef, Source[T, NotUsed]) = {
    val actorRefSource: Source[T, ActorRef] = Source.actorRef[T](bufferSize = 1000, OverflowStrategy.fail)
    actorRefSource.toMat(BroadcastHub.sink[T](bufferSize = 256))(Keep.both).run()
  }

}

