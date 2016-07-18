package org.denigma.kappa.messages

import boopickle.DefaultBasic._
import boopickle.CompositePickler
import scala.collection.immutable._

object KappaFileMessage {
  implicit val kappaFilePickler: CompositePickler[KappaFileMessage] = compositePickler[KappaFileMessage]
    .addConcreteType[DataChunk]
    .addConcreteType[DataMessage]
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
    .addConcreteType[KappaFile]
    .addConcreteType[KappaFolder]
}

sealed trait KappaPath extends KappaFileMessage
{
  def path: String
  def saved: Boolean
}

object KappaFolder {

  implicit val classPickler: Pickler[KappaFolder] = boopickle.Default.generatePickler[KappaFolder]

  implicit val ordering: Ordering[KappaFolder] = new Ordering[KappaFolder] {
    override def compare(x: KappaFolder, y: KappaFolder): Int = x.path.compare(y.path) match {
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
  //lazy val childFiles = children.collect{case f: KappaFile => f}
  //lazy val childFolders = children.collect{case f: KappaFolder => f}

}


object FileType extends Enumeration {
  type FileType = Value
  val pdf, txt, source, image, video, other = Value
}

object KappaFile
{
  implicit val classPickler: Pickler[KappaFile] = boopickle.Default.generatePickler[KappaFile]

  implicit val ordering: Ordering[KappaFile] = new Ordering[KappaFile] {
    override def compare(x: KappaFile, y: KappaFile): Int = x.name.compare(y.name) match {
      case 0 =>
        x.path.compare(y.path) match {
          case 0 => x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
          case other => other
        }
      case other => other
    }
  }
}

/*
* TODO:
* */
case class KappaFile(path: String, name: String, content: String, saved: Boolean = false, active: Boolean = true) extends KappaPath {

  lazy val fileType: FileType.Value = name match {
    case n if n.endsWith(".pdf") => FileType.pdf
    case n if n.endsWith(".txt") => FileType.txt
    case n if n.endsWith(".ka") | n.endsWith(".ttl") => FileType.source
    case n if n.endsWith(".svg") | n.endsWith(".png") | n.endsWith(".jpg") | n.endsWith(".gif") => FileType.image
    case n if n.endsWith(".avi") => FileType.video
    case other => FileType.other
  }

  /*
  def relativeTo(parentPath: String): KappaFile = {
    val np = (parentPath, path) match {
      case (par, me) if par.endsWith("/") &&  me.startsWith("/") => par + me.tail
      case (par, me) if par.endsWith("/") => par + me
      case (par, me) if me.startsWith("/") => par + me
      case (par, me) => par + "/" +me
    }
    this.copy( path = np )
  }


  lazy val fullPath: String = path match {
    case p if p.endsWith(name) => p
    case p if p.endsWith("/") || p.endsWith("\\") => p + name
    case p => p + "/" + name
  }
  */
}

object FileRequests {


  implicit val pickler: CompositePickler[FileRequest] = compositePickler[FileRequest]
    .addConcreteType[FileRequests.UploadBinary]
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

  case class Remove(projectName: String, filename: String) extends FileRequest

  object LoadFileSync {
    implicit val classPickler: Pickler[LoadFileSync] = boopickle.Default.generatePickler[LoadFileSync]
  }

  case class LoadFileSync(projectName: String, path: String) extends FileRequest

  object LoadBinaryFile{
    implicit val classPickler: Pickler[LoadBinaryFile] = boopickle.Default.generatePickler[LoadBinaryFile]
  }

  case class LoadBinaryFile(projectName: String, path: String, chunkSize: Int = 8192 * 8 /*-1*/) extends FileRequest

  object UploadBinary{
    implicit val classPickler: Pickler[UploadBinary] = boopickle.Default.generatePickler[UploadBinary]
  }

  case class UploadBinary(projectName: String, files: List[DataMessage]) extends FileRequest

  object Rename{
    implicit val classPickler: Pickler[Rename] = boopickle.Default.generatePickler[Rename]
    def apply(projectName: String): Rename = Rename(projectName, Map.empty)
  }

  case class Rename(projectName: String, renames: Map[String, String], rewriteIfExists: Boolean = false) extends FileRequest

  object Save{
    implicit val classPickler: Pickler[Save] = boopickle.Default.generatePickler[Save]
  }

  case class Save(projectName: String, files: List[KappaFile], rewrite: Boolean, getSaved: Boolean = false) extends FileRequest

  object ZipUpload{
    implicit val classPickler: Pickler[ZipUpload] = boopickle.Default.generatePickler[ZipUpload]
  }

  case class ZipUpload(projectName: String, zip: DataMessage, rewriteIfExist: Boolean = false) extends FileRequest
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

  case class UploadStatus(projectName: String, hash: Int, rewriteIfExist: Boolean) extends FileResponse

  object RenamingResult {
    implicit val classPickler: Pickler[RenamingResult] = boopickle.Default.generatePickler[RenamingResult]
  }

  case class RenamingResult(projectName: String, renamed: Map[String, (String, String)], nameConflicts: Map[String, String], notFound: Map[String, String]) extends FileResponse

  object SavedFiles {
    implicit val classPickler: Pickler[SavedFiles] = boopickle.Default.generatePickler[SavedFiles]
  }

  case class SavedFiles(projectName: String, savedFiles: Either[Set[String], Map[String, KappaFile]]) extends FileResponse

  object FileAdded {
    implicit val classPickler: Pickler[FileAdded] = boopickle.Default.generatePickler[FileAdded]
  }

  case class FileAdded(projectName: String, files: List[KappaFile]) extends FileResponse

}

object DataChunk{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[DataChunk] = boopickle.Default.generatePickler[DataChunk]
}

case class DataChunk(id: KappaMessage, path: String, data: Array[Byte], downloaded: Int, total: Int, completed: Boolean = false) extends KappaFileMessage
{
  lazy val percent = downloaded / total
}

object DataMessage{
  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[DataMessage] = boopickle.Default.generatePickler[DataMessage]
}

case class DataMessage(name: String, data: Array[Byte]) extends KappaFileMessage


