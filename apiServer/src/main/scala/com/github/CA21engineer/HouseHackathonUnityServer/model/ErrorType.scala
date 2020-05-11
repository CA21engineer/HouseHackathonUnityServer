package com.github.CA21engineer.HouseHackathonUnityServer.model

sealed class ErrorType(val message: String)
case object RoomNotFound extends ErrorType("RoomNotFound")
case object LostConnection extends ErrorType("LostConnection")
case object MalformedMessageType extends ErrorType("MalformedMessageType")
