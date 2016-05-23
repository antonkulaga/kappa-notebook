package org.denigma.kappa.notebook

import java.io.{File => JFile}
import java.nio.file.Path

import better.files.File.OpenOptions
import better.files._
import org.denigma.kappa.messages.FileRequests.ZipUpload
import org.denigma.kappa.messages.FileResponses.Downloaded
import org.denigma.kappa.messages._

import scala.Seq
import scala.collection.immutable._
import scala.util.Try


class FileManager(val root: File) {

  def create(project: KappaProject, rewriteIfExists: Boolean = false): File = {
    write(project.folder)
  }

  def remove(project: String, name: String): File = {
    val path: File = root / project / name
    path.delete()
  }

  def remove(name: String): File = {
    val path: File = root / name
    path.delete()
  }


  def loadProjectSet(): SortedSet[KappaProject] = {
    SortedSet(root.children.collect{
      case child if child.isDirectory => KappaProject(child.name)//loadProject(child.path)
    }.toSeq:_*)
  }

  protected def projectExists(path: String): Boolean = (root / path).exists

  def loadZiped(projectName: String): Option[Downloaded] =  if(projectExists(projectName)){
    val zp = (root / projectName).zip()
    Some(FileResponses.Downloaded(projectName, zp.byteArray))
  } else None

  def uploadZiped(upload: ZipUpload) =  {
    val r: File = root / upload.projectName
    if(r.exists && !upload.rewriteIfExist)
    {
      None
    } else {
      if( r.exists && upload.rewriteIfExist) r.delete()
      val dir = r.createDirectory()
      val tmp = File.newTemporaryFile().write(upload.zip.data)(better.files.File.OpenOptions.default)
      tmp.unzipTo(dir)
      Some(
        FileResponses.UploadStatus(
          upload.projectName,
          upload.zip.data.hashCode(),
          upload.rewriteIfExist))
    }
  }

  def getJavaPath(relativePath: String): Option[(Path, Int)] = {
    val file = root / relativePath
    if(file.exists && file.isRegularFile) {
      Some(file.path, file.toJava.length.toInt)
    } else None
  }

  def getJavaPath(relativePath: String, filename: String): Option[(Path, Int)] = {
    val file = root / relativePath / filename
    if(file.exists && file.isRegularFile) {
      Some(file.path, file.toJava.length.toInt)
    } else None
  }



  def writeBytes(relativePath: String, name: String, bytes: Array[Byte]): Try[Unit] = Try {
    val f = root / relativePath / name
    f.write(bytes)(OpenOptions.default)
  }

  def readBytes(currentProject: String, relativePath: String): Option[Array[Byte]] = {
    val file = root / currentProject / relativePath
    if(file.exists && file.isRegularFile) {
      Some(file.loadBytes)
    } else None
  }

  def readBytes(relativePath: String): Option[Array[Byte]] = {
    val file = root / relativePath
    if(file.exists && file.isRegularFile) {
      Some(file.loadBytes)
    } else None
  }


  def loadProject(project: KappaProject, createIfNotExists: Boolean = false): KappaProject = {
    val path: File = root / project.name
    val p = if (createIfNotExists) path.createDirectory() else path
    val dir = listFolder(p)
    project.copy(folder = dir, saved = p.exists())
  }

  /**
    * Writes KappaFile to the disk
    *
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

  def readText(relativePath: String): String = (root / relativePath).contentAsString

  def cd(relativePath: String): FileManager = new FileManager(root / relativePath)

}