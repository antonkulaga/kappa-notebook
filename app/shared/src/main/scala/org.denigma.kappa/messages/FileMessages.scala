package org.denigma.kappa.messages

import scala.collection.immutable._

object KappaPath{
  implicit val ordering: Ordering[KappaPath] = new Ordering[KappaPath] {
    override def compare(x: KappaPath, y: KappaPath): Int = x.path.compare(y.path) match {
      case 0 => x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
      case other => other
    }
  }

  lazy val empty: KappaPath = KappaFolder.empty
}

object KappaFolder {

  implicit val ordering: Ordering[KappaFolder] = new Ordering[KappaFolder] {
    override def compare(x: KappaFolder, y: KappaFolder): Int = x.path.compare(y.path) match {
      case 0 => x.hashCode().compare(y.hashCode()) //just to avoid annoying equality bugs
      case other => other
    }
  }

  lazy val empty: KappaFolder = KappaFolder("", SortedSet.empty[KappaFolder], SortedSet.empty[KappaFile], active = false)
}

case class KappaFolder(path: String,
                       folders: SortedSet[KappaFolder] = SortedSet.empty,
                       files: SortedSet[KappaFile], active: Boolean = false) extends KappaPath
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

case class KappaFile(path: String, name: String, content: String, active: Boolean = false) extends KappaPath

sealed trait KappaPath extends KappaFileMessage
{
  def path: String
  def active: Boolean
}


object KappaProject {
  lazy val default: KappaProject = KappaProject("repressilator")

  implicit val ordering = new Ordering[KappaProject] {
    override def compare(x: KappaProject, y: KappaProject): Int = x.name.compare(y.name) match {
      case 0 =>
        x.hashCode().compare(y.hashCode())
      case other => other
    }
  }
}


case class KappaProject(name: String, folder: KappaFolder = KappaFolder.empty) extends KappaFileMessage
{
  def loaded = folder != KappaFolder.empty

  lazy val sources: SortedSet[KappaFile] = folder.files.filter(f=> f.name.endsWith(".ka") || f.name.endsWith(".ttl") )

  lazy val sourceMap: Map[String, KappaFile] = sources.map(f=> (f.name, f)).toMap

  lazy val papers = folder.files.filter(f => f.name.endsWith(".pdf"))

  lazy val images = folder.files.filter(f =>
    f.name.endsWith(".svg") ||
      f.name.endsWith(".gif") ||
      f.name.endsWith(".jpg") ||
      f.name.endsWith(".png") ||
      f.name.endsWith(".webp")
  )
}


case class Load(project: KappaProject = KappaProject.default)  extends KappaFileMessage

object Loaded {
  lazy val empty: Loaded = Loaded(KappaProject.default)
}

case class Loaded(project: KappaProject, other: List[KappaProject] = Nil) extends KappaFileMessage

case class Save(project: KappaProject) extends KappaFileMessage