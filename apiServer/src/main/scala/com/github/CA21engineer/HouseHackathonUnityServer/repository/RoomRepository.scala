package com.github.CA21engineer.HouseHackathonUnityServer.repository

import com.github.CA21engineer.HouseHackathonUnityServer.model.Room

import scalikejdbc._

object RoomRepository {

  def create(roomId: String)(implicit s: DBSession = AutoSession): Unit = {
    sql"INSERT INTO rooms(id) VALUES (${roomId})"
      .update().apply()
  }

  def findByRoomId(roomId: String)(implicit s: DBSession = AutoSession): Seq[Room] = {
    sql"SELECT id FROM rooms WHERE id = ${roomId}".map(rs => {
      Room(rs.string(roomId))
    }).list().apply()
  }
}