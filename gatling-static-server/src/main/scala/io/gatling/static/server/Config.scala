package io.gatling.static.server

import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by zcfrank1st on 13/01/2017.
  */
trait ConfigModule {
  val conf: Config = ConfigFactory.load("static-server")
}
