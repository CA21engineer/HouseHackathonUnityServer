package com.github.CA21engineer.HouseHackathonUnityServer.model

// Up, Down, Left, Right
case class Direction(direction: String)

object Direction {
  def all: List[Direction] = List(Direction("Up"), Direction("Down"), Direction("Left"), Direction("Right"))

  def shuffle: List[Direction] = scala.util.Random.shuffle(all)
}
