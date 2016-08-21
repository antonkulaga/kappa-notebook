package org.denigma.kappa.notebook

import java.io.{File => JFile}

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import better.files._
import com.typesafe.config.Config
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import net.ceedubs.ficus.Ficus._


object Main extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system)
    .withInputBuffer(
      initialSize = 64,
      maxSize = 512))

  implicit val executionContext = system.dispatcher

  val server: HttpExt = Http(system)
  val config: Config = system.settings.config

  val host = config.getString("app.host")
  val port = config.getInt("app.port")
  //val key = config.as[Option[String]]("app.key").getOrElse("files/")
  //val keyFiles =

  val filePath: String = sys.env.get("KAPPA_FILES") match {
    case Some(str) =>
      system.log.info("KAPPA FILES LOCATION IS TAKEN FROM KAPPA_FILES ENVIROMENT VARIABLE")
      str
    case other => config.as[Option[String]]("app.files").getOrElse("files/")
  }
  val root = File(filePath)
  root.createIfNotExists(asDirectory = true)
  val router = new Router(File(filePath))
  val bindingFuture = server.bindAndHandle(router.routes, host, port)(materializer)
  system.log.info(s"starting server at $host:$port")

}