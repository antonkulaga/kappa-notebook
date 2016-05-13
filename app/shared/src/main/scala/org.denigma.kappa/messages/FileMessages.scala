package org.denigma.kappa.messages

import boopickle.Default._

import scala.collection.immutable._

object KappaPath{
  implicit val ordering: Ordering[KappaPath] = new Ordering[KappaPath] {
    override def compare(x: KappaPath, y: KappaPath): Int = x.path.compare(y.path) match {
      case 0 => x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
      case other => other
    }
  }

  lazy val empty: KappaPath = KappaFolder.empty

  implicit val kappaPathPickler = compositePickler[KappaPath]
    .addConcreteType[KappaFile]
    .addConcreteType[KappaFolder]
}

sealed trait KappaPath extends KappaFileMessage
{
  def path: String
  def saved: Boolean
}

object KappaFolder {

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

object KappaFile
{
  implicit val ordering: Ordering[KappaFile] with Object {def compare(x: KappaFile, y: KappaFile): Int} = new Ordering[KappaFile] {
    override def compare(x: KappaFile, y: KappaFile): Int = x.path.compare(y.path) match {
      case 0 => x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
      case other => other
    }
  }
}

case class KappaFile(path: String, name: String, content: String, saved: Boolean = false) extends KappaPath




object KappaProject {

  lazy val default: KappaProject = KappaProject("presentation", saved = false)

  implicit val ordering = new Ordering[KappaProject] {
    override def compare(x: KappaProject, y: KappaProject): Int = x.name.compare(y.name) match {
      case 0 =>
        x.hashCode().compare(y.hashCode())
      case other => other
    }
  }
}


case class KappaProject(name: String, folder: KappaFolder = KappaFolder.empty, saved: Boolean = false) extends KappaFileMessage
{
  def loaded = folder != KappaFolder.empty

  protected def sourceFilter(f: KappaFile) =  f.name.endsWith(".ka") || f.name.endsWith(".ttl")

  protected def imageFilter(f: KappaFile) =   f.name.endsWith(".svg") ||
    f.name.endsWith(".gif") ||
    f.name.endsWith(".jpg") ||
    f.name.endsWith(".png") ||
    f.name.endsWith(".webp")


  protected def videoFilter(f: KappaFile) =   f.name.endsWith(".avi") ||
    f.name.endsWith(".mp4") ||
    f.name.endsWith(".flv") ||
    f.name.endsWith(".mpeg")


  lazy val sources: SortedSet[KappaFile] = folder.files.filter(sourceFilter)

  lazy val sourceMap: Map[String, KappaFile] = sources.map(f=> (f.name, f)).toMap

  lazy val papers = folder.files.filter(f => f.name.endsWith(".pdf"))

  lazy val images = folder.files.filter(imageFilter)

  lazy val videos = folder.files.filter(videoFilter)

  lazy val nonsourceFiles = folder.files.filterNot(sourceFilter)
}
object FileRequests {
  //case class Update(project: KappaProject, insertions) extends KappaFileMessage
  case class Remove(projectName: String) extends KappaFileMessage
  case class Create(project: KappaProject, rewriteIfExists: Boolean = false) extends KappaFileMessage
  case class Load(project: KappaProject = KappaProject.default) extends KappaFileMessage
  case class LoadFile(path: String) extends KappaFileMessage

  //case class Upload() extends KappaFileMessage
  case class Download(projectName: String) extends KappaFileMessage
  case class Save(project: KappaProject) extends KappaFileMessage
  case class Upload(projectName: String, filename: String, data: Array[Byte]) extends KappaFileMessage
  case class ZipUpload(projectName: String, data: Array[Byte], rewriteIfExist: Boolean = false) extends KappaFileMessage
}
object FileResponses {
  object Loaded {
    def apply(projects: List[KappaProject]): Loaded = Loaded(None, SortedSet(projects:_*))
    lazy val empty: Loaded = Loaded(Nil)
  }

  case class Loaded(projectOpt: Option[KappaProject], projects: SortedSet[KappaProject] = SortedSet.empty) extends KappaFileMessage {
    lazy val project = projectOpt.getOrElse(KappaProject.default) //TODO fix this broken thing!!!!!
  }

  object Downloaded {
    lazy val empty = Downloaded("", Array())
  }
  case class Downloaded(folderName: String, data: Array[Byte]) extends KappaFileMessage

  case class UploadStatus(projectName: String, hash: Int, rewriteIfExist: Boolean) extends KappaFileMessage

}








