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
import org.denigma.kappa.messages.{KappaFile, KappaFolder, KappaPath, KappaProject}

import scala.collection.immutable.SortedSet
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
  root.createIfNotExists(asDirectory = true)
  val router = new Router(File(filePath))
  val bindingFuture = server.bindAndHandle(router.routes, host, port)(materializer)

  println(s"starting server at $host:$port")


}

object FileManager {

}

class FileManager(root: File) {


  def loadProject(project: KappaProject): KappaPath = {
    val path = root / project.name
    listFolder(path)
  }


  def listFolder(file: File, knownExtensions: Set[String] = Set("ka", "txt", "ttl", "sbol")): KappaFolder = {
    val lst = file.children.map{
      case ch if ch.isDirectory => listFolder(ch, knownExtensions)
      case ch if ch.isRegularFile && knownExtensions.contains(ch.pathAsString.substring(ch.pathAsString.indexOf(".")) +1) =>
        KappaFile(ch.pathAsString, ch.name, ch.contentAsString, true)
      case ch if ch.isRegularFile => KappaFile(ch.pathAsString, ch.name, "",  false)
    }.toSeq
    val children: SortedSet[KappaPath] = SortedSet(lst:_*)
    KappaFolder(file.pathAsString, children)
  }
/*
  def listFolder(kappaPath: KappaPath: File, parent: Option[File] = None, knownExtensions: Set[String] = Set("ka", "txt", "ttl", "sbol")): KappaFolder = {
    KappaFolder.empty.copy(name)
    val lst = file.children.map{
      case ch if ch.isDirectory => listFolder(ch, Some(file))
      case ch if ch.isRegularFile && knownExtensions.contains(ch.pathAsString.substring(ch.pathAsString.indexOf(".")) +1) =>
        KappaFile(ch.pathAsString, ch.name, ch.contentAsString, None, false)
      case ch if ch.isRegularFile => KappaFile(ch.pathAsString, ch.name, "", None, false)
    }.toSeq
    val children = SortedSet(lst:_*)
    KappaFolder(file.pathAsString, children, parent.map(f => KappaPath(f.pathAsString)))
  }
  */


  def read(relativePath: String): String = (root / relativePath).contentAsString

  def cd(relativePath: String): FileManager = new FileManager(root / relativePath)


  def read(file: File) =  file.contentAsString

}