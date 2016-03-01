package org.denigma.kappa.notebook

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config


object Main extends App  {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val router = new Router()

  val server: HttpExt = Http(system)
  val config: Config = system.settings.config

  val (host, port) = (config.getString("app.host"), config.getInt("app.port"))
  println(s"starting server at $host:$port")
  server.bindAndHandle(router.routes, host, port)

  val bindingFuture = server.bindAndHandle(router.routes, host, port)(materializer)
  println(s"starting server at $host:$port")
}

