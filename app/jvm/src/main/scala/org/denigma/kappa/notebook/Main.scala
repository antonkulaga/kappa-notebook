package org.denigma.kappa.notebook

import java.io.{File => JFile}

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import better.files._
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import org.denigma.kappa.messages.{KappaFile, KappaFolder, KappaProject}

import scala.Seq
import scala.collection.immutable._


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
  system.log.info(s"starting server at $host:$port")

}

object FileManager {

}

class FileManager(val root: File) {

  def remove(name: String) = {
    val path: File = root / name
    path.delete()
  }


  def loadProjectSet(): Set[KappaProject] = {
    root.children.collect{
      case child if child.isDirectory => KappaProject(child.name)//loadProject(child.path)
    }.toSet
  }

  def loadProject(project: KappaProject, createIfNotExists: Boolean = false): KappaProject = {
    val path: File = root / project.name
    val p = if (createIfNotExists) path.createDirectory() else path
    val dir = listFolder(p)
    project.copy(folder = dir, saved = p.exists)
  }


  def listFolder(file: File, knownExtensions: Set[String] = Set("ka", "txt", "ttl", "sbol")): KappaFolder =
  if(file.exists)
  {
    val (folders: Iterator[File], files: Iterator[File]) = file.children.partition(f => f.isDirectory)
    val fiter: Seq[KappaFile] =  files.map{
      case ch if ch.isRegularFile
        && {
        val p = ch.pathAsString
        val ext = p.substring(ch.pathAsString.indexOf(".") +1)
        knownExtensions.contains(ext)
      }  =>
        KappaFile(ch.pathAsString, ch.name, ch.contentAsString, saved = true)
      case ch if ch.isRegularFile => KappaFile(ch.pathAsString, ch.name, "",  saved = true)
    }.toSeq
    val diter = folders.map{ case ch => listFolder(ch, knownExtensions) }.toSeq
    val dirs = SortedSet(diter:_*)
    KappaFolder(file.pathAsString, dirs, SortedSet(fiter:_*))
  } else KappaFolder(file.pathAsString, SortedSet.empty, SortedSet.empty, saved = false)
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