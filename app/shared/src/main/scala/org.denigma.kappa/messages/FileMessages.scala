package org.denigma.kappa.messages

import java.nio.ByteBuffer

import boopickle.DefaultBasic._
import boopickle.CompositePickler

import scala.collection.immutable._

object KappaFileMessage {
  implicit val kappaFilePickler: CompositePickler[KappaFileMessage] = compositePickler[KappaFileMessage]
    .addConcreteType[DataChunk]
    .join(FileResponses.pickler)
    .join(FileRequests.pickler)
    .join(KappaProject.projectPickler)
    .join(KappaPath.kappaPathPickler)

}

trait KappaFileMessage extends KappaMessage


object KappaPath{
  implicit val ordering: Ordering[KappaPath] = new Ordering[KappaPath] {
    override def compare(x: KappaPath, y: KappaPath): Int = x.path.compare(y.path) match {
      case 0 => x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
      case other => other
    }
  }

  lazy val empty: KappaPath = KappaFolder.empty

  implicit val kappaPathPickler: CompositePickler[KappaPath] = compositePickler[KappaPath]
    .addConcreteType[KappaFolder]
    .join(KappaFile.classPickler)
}

sealed trait KappaPath extends KappaFileMessage
{
  def path: String
  def saved: Boolean

  def hasNoName: Boolean = name == ""

  lazy val pathSegments: List[String] = path.split("[/\\\\]").toList

  lazy val path2Me = pathSegments.take(pathSegments.length -1)

  lazy val name = pathSegments.last

  def isInside(p: KappaFolder): Boolean = {
    val l = path2Me.length
    path2Me == p.pathSegments.take(l)
  }

  def isDirectChildOf(folder: KappaFolder) = isInside(folder) && path2Me.last == folder.name
}

case class FileNotFound(path: String, folderPath: String) extends Exception with KappaMessage

object KappaFolder {

  implicit val classPickler: Pickler[KappaFolder] = boopickle.Default.generatePickler[KappaFolder]

  implicit val ordering: Ordering[KappaFolder] = new Ordering[KappaFolder]{
    def compare(x: KappaFolder, y: KappaFolder) = x.path.compare(y.path) match {
    case 0 => x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
    case other => other
    }
  }

  lazy val empty: KappaFolder = KappaFolder("", SortedSet.empty[KappaFolder], SortedSet.empty[KappaFile])
}



case class KappaFolder(path: String,
                       folders: SortedSet[KappaFolder] = SortedSet.empty,
                       files: SortedSet[KappaFile], saved: Boolean = false) extends KappaPath
{
  self =>

  def markSaved(pathes: List[String]) = self.mapFiles{
    case b: KappaBinaryFile if !b.saved && pathes.contains(b.path) => b.copy(saved = true)
    case s: KappaSourceFile if !s.saved && pathes.contains(s.path) => s.copy(saved = true)
    case other => other
  }

  def mapFiles(fun: KappaFile => KappaFile): KappaFolder = {
    val fs = files.map(f => fun(f))
    val dirs = folders.map(dir => dir.mapFiles(fun))
    this.copy(files = fs, folders = dirs)
  }

  def collect(folderPartial: PartialFunction[KappaFolder, KappaFolder])(filePartial: PartialFunction[KappaFile, KappaFile]): KappaFolder = {
    val fs: SortedSet[KappaFile] = files.collect(filePartial)
    val dirs = folders.collect{folderPartial}.map(d=>d.collect(folderPartial)(filePartial))
    this.copy(files = fs, folders = dirs)
  }


  def moveTo(newPath: String, rename: Boolean = false): KappaFolder = {
    val np = (newPath, rename) match {
      case (n, true) if n.endsWith("/") => n.dropRight(1)
      case (n, false) if n.endsWith("/") => n + name
      case (n, true) => n
      case (n, false) => n + "/" +name
    }
    val newFolders: SortedSet[KappaFolder] = folders.map{ f => f.moveTo(np) }
    val newFiles: SortedSet[KappaFile] = files.map{
      case s: KappaSourceFile => s.moveTo(np, false)
      case b: KappaBinaryFile => b.moveTo(np, false)
    }
    this.copy(path = np, newFolders, newFiles)
  }

  def addFiles(fs: List[KappaFile]): KappaFolder = {
   val (right, wrong) = fs.partition(f => f.isInside(this))
    if(wrong.nonEmpty) println(s"WRONG FILES INSIDE ${path} ARE = "+wrong.mkString(" | "))
    val ps = right.map(_.path).toSet
    //merge is bad because subfolders are not take into account
    this.copy(files = self.files.filterNot(f => ps.contains(f.path)) ++ right)
  }

  def removeFiles(fs: Set[String]) = {
    this.copy(files = files.filterNot(f => fs.contains(f.path)))
  }

  def withFolders(folders: List[KappaFolder]): KappaFolder= {
    ???
  }

  lazy val allFiles: SortedSet[KappaFile] = files ++ folders.flatMap(f=>f.allFiles)

  lazy val allFilesMap: Map[String, KappaFile] = allFiles.map(f => (f.path, f)).toMap
}


object FileType extends Enumeration {
  type FileType = Value
  val pdf, txt, source, image, video, other = Value
}

object KappaSourceFile
{
  implicit val classPickler: Pickler[KappaSourceFile] = boopickle.Default.generatePickler[KappaSourceFile]

  implicit val ordering: Ordering[KappaSourceFile] =  new Ordering[KappaSourceFile]
  {
    def compare(x: KappaSourceFile, y: KappaSourceFile) = x.name.compare(y.name) match {
      case 0 =>
        x.path.compare(y.path) match {
          case 0 =>
            x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
          case other => other
        }
      case other => other
    }
  }
}

/*
* TODO:
* */
case class KappaSourceFile(path: String, content: String, saved: Boolean = false, active: Boolean = true) extends KappaFile
{
 override def asSaved  = this.copy(saved = true)

  def moveTo(newPath: String, rename: Boolean = false) = if(rename){
    this.copy(path = newPath)
  } else this.copy(path = newPath + "/" + name)

}

object KappaBinaryFile {
  implicit val classPickler: Pickler[KappaBinaryFile] = boopickle.Default.generatePickler[KappaBinaryFile]

  implicit val ordering: Ordering[KappaBinaryFile] = new Ordering[KappaBinaryFile]
  {
    def compare(x: KappaBinaryFile, y: KappaBinaryFile) = x.name.compare(y.name) match {
      case 0 =>
        x.path.compare(y.path) match {
          case 0 =>
            x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
          case other => other
        }
      case other => other
    }
  }

}
case class KappaBinaryFile(path: String, content: ByteBuffer, saved: Boolean = false, active: Boolean = true) extends KappaFile {
  def isEmpty = content.limit == 0
  override def asSaved = this.copy(saved = true)

  def moveTo(newPath: String, rename: Boolean = false) = if(rename){
    this.copy(path = newPath)
  } else this.copy(path = newPath + "/" + name)
}

object KappaFile {
  implicit val classPickler: CompositePickler[KappaFile] = compositePickler[KappaFile]
    .addConcreteType[KappaSourceFile]
    .addConcreteType[KappaBinaryFile]

  implicit val ordering: Ordering[KappaFile] = new Ordering[KappaFile]
  {
    def compare(x: KappaFile, y: KappaFile) = x.name.compare(y.name) match {
      case 0 =>
        x.path.compare(y.path) match {
          case 0 =>
            x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
          case other => other
        }
      case other => other
    }
  }

}

trait KappaFile extends KappaPath {
  def path: String
  def saved: Boolean
  def active: Boolean

  lazy val fileType: FileType.Value = name match {
    case n if n.endsWith(".pdf") => FileType.pdf
    case n if n.endsWith(".txt") => FileType.txt
    case n if n.endsWith(".ka") | n.endsWith(".ttl") => FileType.source
    case n if n.endsWith(".svg") | n.endsWith(".png") | n.endsWith(".jpg") | n.endsWith(".gif") => FileType.image
    case n if n.endsWith(".avi") => FileType.video
    case other => FileType.other
  }

  def asSaved: KappaFile
}


object FileRequests {


  implicit val pickler: CompositePickler[FileRequest] = compositePickler[FileRequest]
    .addConcreteType[FileRequests.LoadBinaryFile]
    .addConcreteType[FileRequests.LoadFileSync]
    .addConcreteType[FileRequests.Remove]
    .addConcreteType[FileRequests.Rename]
    .addConcreteType[FileRequests.Save]
    .addConcreteType[FileRequests.ZipUpload]


  trait FileRequest extends KappaFileMessage

  object Remove {
    implicit val classPickler: Pickler[Remove] = boopickle.Default.generatePickler[Remove]
  }

  case class Remove(pathes: Set[String]) extends FileRequest

  object LoadFileSync {
    implicit val classPickler: Pickler[LoadFileSync] = boopickle.Default.generatePickler[LoadFileSync]
  }

  case class LoadFileSync(path: String) extends FileRequest

  object LoadBinaryFile{
    implicit val classPickler: Pickler[LoadBinaryFile] = boopickle.Default.generatePickler[LoadBinaryFile]
  }

  case class LoadBinaryFile(path: String, chunkSize: Int = 8192 * 8 /*-1*/) extends FileRequest

  object Rename{
    implicit val classPickler: Pickler[Rename] = boopickle.Default.generatePickler[Rename]
  }

  case class Rename(renames: Map[String, String], rewriteIfExists: Boolean = false) extends FileRequest

  object Save{
    implicit val classPickler: Pickler[Save] = boopickle.Default.generatePickler[Save]
  }

  case class Save(files: List[KappaFile], rewrite: Boolean, getSaved: Boolean = false) extends FileRequest
  {
    lazy val sources = files.collect{ case f: KappaSourceFile =>f}
    lazy val binaries = files.collect{ case f: KappaBinaryFile =>f}

  }

  object ZipUpload{
    implicit val classPickler: Pickler[ZipUpload] = boopickle.Default.generatePickler[ZipUpload]
  }

  case class ZipUpload(zip: KappaBinaryFile, rewriteIfExist: Boolean = false) extends FileRequest
}

object FileResponses {


  implicit val pickler: CompositePickler[FileResponse] = compositePickler[FileResponse]
    .addConcreteType[Downloaded]
    .addConcreteType[UploadStatus]
    .addConcreteType[SavedFiles]
    .addConcreteType[RenamingResult]
    .addConcreteType[FileAdded]

  trait FileResponse extends KappaFileMessage

  object Downloaded{
    implicit val classPickler: Pickler[Downloaded] = boopickle.Default.generatePickler[Downloaded]

    lazy val empty = Downloaded("", Array())
  }
  case class Downloaded(folderName: String, data: Array[Byte]) extends FileResponse

  object UploadStatus{
    implicit val classPickler: Pickler[UploadStatus] = boopickle.Default.generatePickler[UploadStatus]
  }

  case class UploadStatus(path: String, hash: Int, rewriteIfExist: Boolean) extends FileResponse

  object RenamingResult {
    implicit val classPickler: Pickler[RenamingResult] = boopickle.Default.generatePickler[RenamingResult]
  }

  case class RenamingResult(renamed: Map[String, (String, String)], nameConflicts: Map[String, String], notFound: Map[String, String]) extends FileResponse

  object SavedFiles {
    implicit val classPickler: Pickler[SavedFiles] = boopickle.Default.generatePickler[SavedFiles]
  }

  case class SavedFiles(savedFiles: Either[List[String], List[KappaFile]]) extends FileResponse

  object FileAdded {
    implicit val classPickler: Pickler[FileAdded] = boopickle.Default.generatePickler[FileAdded]
  }

  case class FileAdded(files: List[KappaFile]) extends FileResponse

}

object DataChunk{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[DataChunk] = boopickle.Default.generatePickler[DataChunk]
}

case class DataChunk(id: KappaMessage, path: String, data: Array[Byte], downloaded: Int, total: Int, completed: Boolean = false) extends KappaFileMessage
{
  lazy val percent = downloaded / total
}
