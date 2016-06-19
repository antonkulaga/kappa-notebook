package org.denigma.kappa.messages

import scala.collection.immutable._
import boopickle.DefaultBasic._

object KappaProject {

  implicit val classPickler: Pickler[KappaProject] = boopickle.Default.generatePickler[KappaProject]

  implicit val projectPickler = compositePickler[KappaFileMessage]
    .addConcreteType[ProjectRequests.Create]
    .addConcreteType[ProjectRequests.Download]
    .addConcreteType[ProjectRequests.Load]
    .addConcreteType[ProjectRequests.Save]
    .addConcreteType[ProjectResponses.Loaded]
    .addConcreteType[ProjectRequests.Remove]
    .addConcreteType[KappaProject]

  lazy val default: KappaProject = KappaProject(/*"presentation"*/"repressilator", saved = false)

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
object ProjectRequests {

  object Load {
    implicit val classPickler: Pickler[Load] = boopickle.Default.generatePickler[Load]
  }
  case class Load(project: KappaProject = KappaProject.default) extends KappaFileMessage

  object Save {
    implicit val classPickler: Pickler[Save] = boopickle.Default.generatePickler[Save]
  }

  case class Save(project: KappaProject) extends KappaFileMessage
  object Create {
    implicit val classPickler: Pickler[Create] = boopickle.Default.generatePickler[Create]
  }
  case class Create(project: KappaProject, rewriteIfExists: Boolean = false) extends KappaFileMessage

  object Delete {
    implicit val classPickler: Pickler[Delete] = boopickle.Default.generatePickler[Delete]
  }

  case class Delete(project: KappaProject) extends KappaFileMessage

  object Download {
    implicit val classPickler: Pickler[Download] = boopickle.Default.generatePickler[Download]
  }

  case class Download(projectName: String) extends KappaFileMessage


  object Remove {
    implicit val classPickler: Pickler[Remove] = boopickle.Default.generatePickler[Remove]
  }

  case class Remove(projectName: String) extends KappaFileMessage
}

object ProjectResponses {
  object Loaded {

    def apply(projects: List[KappaProject]): Loaded = Loaded(None, SortedSet(projects:_*))

    implicit val classPickler: Pickler[Loaded] = boopickle.Default.generatePickler[Loaded]

    lazy val empty: Loaded = Loaded(Nil)
  }

  case class Loaded(projectOpt: Option[KappaProject], projects: SortedSet[KappaProject] = SortedSet.empty) extends KappaFileMessage {
    //lazy val project = projectOpt.getOrElse(KappaProject.default) //TODO fix this broken thing!!!!!
  }
}