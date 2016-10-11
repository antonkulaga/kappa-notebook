package org.denigma.kappa.notebook

import java.io.{File => JFile}
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.zip.Deflater

import akka.event.LoggingAdapter
import better.files.File.OpenOptions
import better.files._
import org.denigma.kappa.messages.FileRequests.ZipUpload
import org.denigma.kappa.messages.FileResponses.{Downloaded, UploadStatus}
import org.denigma.kappa.messages._

import scala.Seq
import scala.collection.immutable._
import scala.util.{Failure, Success, Try}
import org.denigma.kappa.notebook.extensions._

import scala.io.Codec

object FileNotInside {
  val FILE_NOT_INSIDE = "NOT_INSIDE"
  def apply(file: File, parent: File): FileNotInside = FileNotInside(file.pathAsString, parent.pathAsString)
}

case class FileNotInside(file: String, parent: String) extends Exception
{
 def message = s"file $file is not inside $parent folder"
}

object FileManager {
  val FILE_NOT_FOUND = "NOT_FOUND"
  val FILE_NOT_INSIDE = "NOT_INSIDE"
}

class FileManager(val root: File, log: LoggingAdapter) {

  log.info(s"FileManager is initialized with at ${root.pathAsString} folder")

  def create(project: KappaProject, rewriteIfExists: Boolean = false): Try[File] = {
    val p = if (project.folder == KappaFolder.empty) project.copy(folder = KappaFolder.empty.copy(path = project.name)) else project
    //log.info(s"write project folder ${p.folder.path}")
    writeFolder(p.folder, root)
  }
  
  def rename(renames: Map[String, String], overwriteIfExists: Boolean = false): FileResponses.RenamingResult = {
    
    renames.foldLeft(FileResponses.RenamingResult( Map.empty, Map.empty, Map.empty)) {
      case (acc, (currentPath, newPath)) =>
        (root resolveChild (currentPath, false), root.resolveChild(currentPath, mustExist = false)) match {
          case (None, _) =>  acc.copy(notFound = acc.notFound.updated(currentPath, newPath))

          case (_ , None) => acc.copy(notFound = acc.notFound.updated(currentPath, newPath))

          case (Some(fromPath), Some(toPath)) if fromPath.notExists=>
            acc.copy(notFound = acc.notFound.updated(currentPath, newPath))

          case (Some(fromPath), Some(toPath)) if toPath.exists && overwriteIfExists=>
            fromPath.renameTo(newPath)
            acc.copy(renamed = acc.renamed.updated(currentPath, (newPath, toPath.pathAsString)))

          case (Some(fromPath), Some(toPath)) if toPath.exists && !overwriteIfExists=>
            acc.copy(nameConflicts = acc.nameConflicts.updated(currentPath, newPath))

          case (Some(fromPath), Some(toPath)) =>
            fromPath.renameTo(newPath)
            acc.copy(renamed = acc.renamed.updated(currentPath, (newPath, toPath.pathAsString)))
        }
    }
  }

  def remove(project: String, name: String): Try[File] = root.resolveChild(project, name, true) match {
    case Some(path) => Try(path.delete())
    case None =>  Failure(FileNotInside(root / project / name, root))
  }

  def remove(name: String): Try[File] = root.resolveChild(name) match {
    case Some(path) => Try(path.delete())
    case None => Failure(FileNotInside((root / name).pathAsString, root.pathAsString))
  }


  def loadProjectSet(): SortedSet[KappaProject] = {
    SortedSet(root.children.collect{
      case child if child.isDirectory && !child.name.startsWith(".") => KappaProject(child.name)//loadProject(child.path)
    }.toSeq:_*)
  }

  def loadZiped(folderName: String): Option[Downloaded] =  root.resolveChild(folderName, mustExist = true) map {
    folder =>
      FileResponses.Downloaded(folderName, folder.compress().byteArray)
  }

  def uploadZiped(upload: ZipUpload): Option[UploadStatus] =  root.resolveChild(upload.zip.path) flatMap {
    case r  if r.exists && !upload.rewriteIfExist => None
    case r =>
      if( r.exists && upload.rewriteIfExist) r.delete()
      val dir = r.createDirectory()
      val buff: ByteBuffer = upload.zip.content
      val arr = buff.array()
      val tmp = File.newTemporaryFile().write(arr)(better.files.File.OpenOptions.default)
      tmp.unzipTo(dir)
      Some(
        FileResponses.UploadStatus(
          upload.zip.path,
          upload.zip.content.hashCode(),
          upload.rewriteIfExist))
  }

  def getJavaPath(relativePath: String): Option[(Path, Int)] = root.resolveChild(relativePath, true) collect {
    case file if file.isRegularFile => (file.path, file.toJava.length.toInt)
  }

  def getJavaPath(relativePath: String, filename: String): Option[(Path, Int)] =root.resolveChild(relativePath, filename, true) collect {
    case file if file.isRegularFile => (file.path, file.toJava.length.toInt)
  }

  def readBytes(currentProject: String, relativePath: String): Option[Array[Byte]] = root.resolveChild(currentProject, relativePath, true) collect {
    case file if file.isRegularFile => file.loadBytes
  }

  def readBytes(relativePath: String): Option[Array[Byte]] = root.resolveChild(relativePath, true) collect {
    case file if file.isRegularFile =>  file.loadBytes
  }


  def loadProject(project: KappaProject, createIfNotExists: Boolean = false) = root.resolveChild(project.name, !createIfNotExists).map{
    path =>
      val p = if (createIfNotExists) path.createDirectory() else path
      val dir = listFolder(p, root)
      project.copy(folder = dir, saved = p.exists())
  }

  def write(p: KappaPath): Try[File] = write(p, root)

  def writeFile(file: KappaFile, parent: File): Try[File] = {
    file match {
      case s: KappaSourceFile =>
        parent.resolveChild(s.path) match {
          case Some(child) =>
            Try {
              child < s.content
              child
            }
          case None => Failure(FileNotInside(file.path, root.pathAsString))
        }
      case b: KappaBinaryFile =>
        parent.resolveChild(b.path) match {
          case Some(child) =>
            Try(child.write(b.content.array())(OpenOptions.default))

          case None => Failure(FileNotInside(file.path, root.pathAsString))
        }
    }
  }
  def writeFolder(folder: KappaFolder, parent: File): Try[File] = folder match {
    case KappaFolder(path, folders, files, _) =>
      parent.resolveChild(path) match {
        case Some(child) =>
          Try{
            //println("files are :"+files.map(f=>f.name->f.path).toList)
            val dir = child.createDirectories()
            for{
              f <- files
            } {
              writeFile(f, child) match {
                case Failure(e) =>
                  log.error(e.getMessage)
                case _ =>  //do nothing on success
              }
            }
            for(f <- folders) writeFolder(f, child)
            dir
          }
        case None => Failure(FileNotInside(path, root.pathAsString))
      }
  }

  /**
    * Writes KappaFile to the disk
    *
    * @param p
    * @return
    */
  def write(p: KappaPath, parent: File): Try[File] = p match {
    case folder: KappaFolder => writeFolder(folder, parent)
    case k: KappaFile => writeFile(k, parent)
  }


  protected def kappaTextFileSelector(ch: File, knownExtensions: Set[String] = Set("ka", "txt", "ttl", "sbol")) = {
    ch.isRegularFile && {
      val p = ch.pathAsString
      val ext = p.substring(ch.pathAsString.indexOf(".") +1)
      knownExtensions.contains(ext)
    }
  }

  protected def loadKappaFile(parent: File, filePath: String,
                              knownExtensions: Set[String] = Set("ka", "txt", "ttl", "sbol")): Option[KappaFile] =
    parent.resolveChild(filePath, mustExist = true).flatMap{
      case file if !kappaTextFileSelector(file, knownExtensions)=> None

      case ch if kappaTextFileSelector(ch, knownExtensions)  =>
        Some(KappaSourceFile(ch.pathAsString, ch.contentAsString, saved = true))

      case ch if ch.isRegularFile =>
        Some(KappaBinaryFile(ch.pathAsString, ByteBuffer.allocate(0),  saved = true))

      case _ => None
    }

  def listFolder(file: File, relativeToParent: File, knownExtensions: Set[String] = Set("ka", "txt", "ttl", "sbol")): KappaFolder = {
    if(file.isChildOf(relativeToParent)) {
      if (file.exists) {
        val (folders: Iterator[File], files: Iterator[File]) = file.children.partition(f => f.isDirectory)
        val fiter: Seq[KappaFile] = files.map {
          case ch if kappaTextFileSelector(ch, knownExtensions) =>

            KappaSourceFile(relativeToParent.relativize(ch).toString, ch.contentAsString, saved = true)

          case ch if ch.isRegularFile =>
            val bytes = ByteBuffer.allocate(0)

            KappaBinaryFile(relativeToParent.relativize(ch).toString, bytes, saved = true)

        }.toSeq

        val diter = folders.map { ch => listFolder(ch, relativeToParent, knownExtensions) }.toSeq

        val dirs = SortedSet(diter: _*)

        KappaFolder(relativeToParent.relativize(file).toString, dirs, SortedSet(fiter: _*))

      }
      else {
        log.error(s"file ${file.pathAsString} does not exist")
        KappaFolder(file.pathAsString, SortedSet.empty, SortedSet.empty[KappaFile], saved = false)
      }
    } else {
      val fni = new FileNotInside(file.pathAsString, relativeToParent.pathAsString)
      log.error(fni, fni.message)
      KappaFolder(file.pathAsString, SortedSet.empty, SortedSet.empty[KappaFile], saved = false)
    }
  }

  def readText(relativePath: String): Option[String] = root.resolveChild(relativePath, mustExist = true).map(_.contentAsString)

  def cd(relativePath: String): Option[FileManager] = root.resolveChild(relativePath, mustExist = true).map(
    child => new FileManager(child, log)
  )
}