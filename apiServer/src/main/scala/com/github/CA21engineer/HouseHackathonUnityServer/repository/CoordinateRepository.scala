package com.github.CA21engineer.HouseHackathonUnityServer.repository

import com.github.CA21engineer.HouseHackathonUnityServer.model.{Room, Coordinate}
import com.github.CA21engineer.HouseHackathonUnityServer.grpc._
import scalikejdbc._

object CoordinateRepository {

  def recordData(roomId: String, datas: Seq[room.Coordinate])(implicit s: DBSession = AutoSession): Unit = {
    datas.foreach(data => create(roomId, data.x ,data.y, data.date))
  }

  def create(roomId: String, x: Float, y: Float, millisec: Long)(implicit s: DBSession = AutoSession): Unit = {
    val id = java.util.UUID.randomUUID.toString.replaceAll("-", "")
    sql"INSERT INTO coordinates(id, room_id, x, y, past_millisecond) VALUES (${id}, ${roomId}, ${x}, ${y}, ${millisec})"
      .update().apply()
  }

  def findByRoomId(roomId: String)(implicit s: DBSession = AutoSession): Seq[Coordinate] = {
    sql"SELECT room_id, x, y, past_millisecond FROM coordinates WHERE room_id=${roomId} order by past_millisecond ASC".map(rs => {
      Coordinate(rs.string("room_id"), rs.float("x"), rs.float("y"), rs.int("past_millisecond"))
    }).list().apply()
  }
}