package com.github.CA21engineer.HouseHackathonUnityServer.apiServer

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._

object Routes {

  def toRoutes: Router = {
    Router(
      route(GET, "health", health)
    )
  }

  def health: Directive[Unit] => Route = _ {
    complete(StatusCodes.OK)
  }

}
