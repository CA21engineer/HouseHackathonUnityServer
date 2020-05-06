package com.github.CA21engineer.HouseHackathonUnityServer.repository

import com.github.CA21engineer.HouseHackathonUnityServer.model.{Room, Coordinate}
import com.github.CA21engineer.HouseHackathonUnityServer.grpc._
import scalikejdbc._

object CoordinateRepository {

  // reorcdData ゴーストレコードの記録
  def recordData(per: Int, roomId: String, datas: Seq[room.Coordinate])(implicit s: DBSession = AutoSession): Unit = {
      var newList = Seq[Seq[room.Coordinate]]()
      var recs = datas
      while(recs.length > per) {
          val n = dividList(per, recs)
          newList :+= n._1
          recs = n._2
      }
      newList.foreach { lst =>
        val params: Seq[Seq[Any]] = lst.map(data => Seq(java.util.UUID.randomUUID.toString.replaceAll("-", ""), roomId, data.x, data.y, data.z, data.date))
        SQL("INSERT INTO coordinates(id, room_id, x, y, z, past_millisecond) VALUES (?,?,?,?,?,?)").batch(params: _*).apply()
      }
      println(s"finish record data!: roomId = ${roomId}")
  }

  def dividList(per: Int, lst :Seq[room.Coordinate]): (Seq[room.Coordinate], Seq[room.Coordinate]) = {
    lst.splitAt(per)
  }

  // create coordinateを一件記録
  def create(roomId: String, x: Float, y: Float, z: Float, millisec: Long)(implicit s: DBSession = AutoSession): Unit = {
    val id = java.util.UUID.randomUUID.toString.replaceAll("-", "")
    sql"INSERT INTO coordinates(id, room_id, x, y, z, past_millisecond) VALUES (${id}, ${roomId}, ${x}, ${y}, ${z}, ${millisec})"
      .update().apply()
  }

  // findByRoomId roomIDからゴーストレコードの取得
  def findByRoomId(roomId: String)(implicit s: DBSession = AutoSession): Seq[room.Coordinate] = {
    sql"SELECT x, y, z, past_millisecond FROM coordinates WHERE room_id=${roomId} order by past_millisecond ASC".map(rs => {
      room.Coordinate(rs.float("x"), rs.float("y"), rs.int("past_millisecond"), rs.float("z"))
    }).list().apply()
  }

  // findBestRecord 最良のゴーストレコードを取得
  def findBestRecord(implicit s: DBSession = AutoSession): Seq[room.Coordinate] = {
    val roomList = sql"SELECT room_id FROM coordinates GROUP BY room_id ORDER BY max(past_millisecond) ASC LIMIT 1"
      .map(_.toMap())
      .list()
      .apply()
    if (roomList.isEmpty) return Seq.empty
    val roomId = roomList.head("room_id").asInstanceOf[String]
    findByRoomId(roomId)
  }

}
