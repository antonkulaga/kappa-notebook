package org.denigma.kappa.notebook

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import better.files._
import java.io.{File => JFile}

import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import org.denigma.kappa.messages.KappaPath

import scala.concurrent.duration.FiniteDuration

object Main extends App  {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val server: HttpExt = Http(system)
  val config: Config = system.settings.config

  val (host, port) = (config.getString("app.host"), config.getInt("app.port"))
  val filePath: String = config.as[Option[String]]("app.files").getOrElse("files/")
  val root = File(filePath)
  root.createIfNotExists(true)
  val fileManager = new FileManager(root)

  val router = new Router(File(filePath))
  val bindingFuture = server.bindAndHandle(router.routes, host, port)(materializer)

  println(s"starting server at $host:$port")


}

object FileManager {

}

class FileManager(root: File) {

  def listFolder(file: File, parent: Option[File] = None): KappaPath = {
    val children = file.children.map{ case ch=> listFolder(ch, Some(file)) }.toList
    KappaPath(file.pathAsString, children, parent.map(f=>KappaPath(f.pathAsString)))
  }

}