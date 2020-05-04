package com.github.CA21engineer.HouseHackathonUnityServer.repository

import com.github.CA21engineer.HouseHackathonUnityServer.model.{Room, Coordinate}

import scalikejdbc._

object CoordinateRepository {

  def create(roomId: String, x: Float, y: Float, millisec: Int)(implicit s: DBSession = AutoSession): Unit = {
    val id = java.util.UUID.randomUUID.toString.toUpperCase()
    sql"INSERT INTO coordinate(id, room_id, x, y, past_millisecond) VALUES (${id}, ${roomId}, ${x}, ${y}, ${millisec})"
      .update().apply()
  }

  def findByRoomId(roomId: String)(implicit s: DBSession = AutoSession): Seq[Room] = {
    sql"SELECT room_id, x, y, past_millisecond FROM coordinates WHERE id=${roomId} order by past_millisecond ASC".map(rs => {
      Coordinate(rs.string("room_id"), rs.float("x"), rs.fload("y"), rs.int("past_millisecond"))
    }).list().apply()
  }
}