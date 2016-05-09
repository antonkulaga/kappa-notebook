package org.denigma.kappa.notebook

import java.io.{File => JFile}

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import better.files._
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import org.denigma.kappa.messages.{KappaFile, KappaFolder, KappaPath, KappaProject}

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

  def create(project: KappaProject, rewriteIfExists: Boolean = false) = {
    write(project.folder)
  }

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

  /**
    * Writes KappaFile to the disk
    * @param p
    * @return
    */
  def write(p: KappaPath): File = p match {
    case KappaFolder(path, folders, files, _) =>
      val dir = File(root.path.resolve(path)).createDirectories()
      for(f <- files) write(f)
      for(f <- folders) write(f)
      dir

    case KappaFile(path, name, content, _) =>
      val f = File(root.path.resolve(path))
      f < content
      f

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

  def read(relativePath: String): String = (root / relativePath).contentAsString

  def cd(relativePath: String): FileManager = new FileManager(root / relativePath)

}