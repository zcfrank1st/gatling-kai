package io.gatling.static.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
/**
  * Created by zcfrank1st on 13/01/2017.
  */
object Main extends ConfigModule {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("gatling-static-server")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val route =
      pathPrefix("") {
        getFromDirectory(conf.getString("static.server.resource"))
      }

    Http().bindAndHandle(route, "0.0.0.0", 9889)
  }
}
