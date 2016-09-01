package org.denigma.kappa.messages

import boopickle.CompositePickler

import scala.collection.immutable._
import boopickle.DefaultBasic._

object KappaProject extends FileFilters{

  implicit val classPickler: Pickler[KappaProject] = boopickle.Default.generatePickler[KappaProject]

  implicit val projectPickler = compositePickler[KappaFileMessage]
    .addConcreteType[KappaProject]
    .join(ProjectResponses.pickler)
    .join(ProjectRequests.projectRequestPickler)

  lazy val default: KappaProject = KappaProject("dna_repair_tutorial"/*"repressilator_light"*/, saved = false)

  lazy val empty: KappaProject = KappaProject("", saved = false)

  implicit val ordering = new Ordering[KappaProject] {
    override def compare(x: KappaProject, y: KappaProject): Int = x.name.compare(y.name) match {
      case 0 =>
        x.folder.path.compare(y.folder.path) match {
          case 0 =>     x.hashCode().compare(y.hashCode())
          case value => value
        }
      case value => value
    }
  }
}



case class KappaProject(name: String, folder: KappaFolder = KappaFolder.empty, saved: Boolean = false) extends KappaFileMessage with FileFilters
{
  self =>

  def loaded = folder != KappaFolder.empty

  lazy val sources: SortedSet[KappaSourceFile] = folder.files.collect{ case f: KappaSourceFile => f}

  lazy val sourceMap: Map[String, KappaSourceFile] = sources.map(f=> (f.path, f)).toMap

  lazy val papers: SortedSet[KappaBinaryFile] = folder.files.collect{ case f: KappaBinaryFile if paperFilter(f) => f}

  lazy val images: SortedSet[KappaBinaryFile] = folder.files.collect{ case f: KappaBinaryFile if imageFilter(f) => f}

  lazy val videos: SortedSet[KappaBinaryFile] = folder.files.collect{ case f: KappaBinaryFile if videoFilter(f) => f}

  lazy val nonsourceFiles: SortedSet[KappaFile] = folder.files.filterNot(sourceFilter)

  lazy val otherFiles: SortedSet[KappaFile] = folder.files
    .filterNot(sourceFilter)
    .filterNot(imageFilter)
    .filterNot(videoFilter)
    .filterNot(paperFilter)
}

trait FileFilters {
  protected def sourceFilter(f: KappaFile): Boolean =  f.name.endsWith(".ka") || f.name.endsWith(".ttl")

  protected def imageFilter(f: KappaFile): Boolean =   f.name.endsWith(".svg") ||
    f.name.endsWith(".gif") ||
    f.name.endsWith(".jpg") ||
    f.name.endsWith(".png") ||
    f.name.endsWith(".webp")


  protected def videoFilter(f: KappaFile): Boolean =   f.name.endsWith(".avi") ||
    f.name.endsWith(".mp4") ||
    f.name.endsWith(".flv") ||
    f.name.endsWith(".mpeg")

  protected def paperFilter(f: KappaFile): Boolean =   f.name.endsWith(".pdf")
}

object ProjectRequests {

  implicit val projectRequestPickler: CompositePickler[ProjectRequest] = compositePickler[ProjectRequest]
    .addConcreteType[ProjectRequests.GetList.type]
    .addConcreteType[ProjectRequests.Create]
    .addConcreteType[ProjectRequests.Download]
    .addConcreteType[ProjectRequests.Load]
    .addConcreteType[ProjectRequests.Save]
    .addConcreteType[ProjectRequests.Remove]


  trait ProjectRequest extends KappaFileMessage

  case object GetList extends ProjectRequest{
    implicit val  classPickler: Pickler[GetList.type] = boopickle.Default.generatePickler[GetList.type]
  }

  object Load {
    implicit val classPickler: Pickler[Load] = boopickle.Default.generatePickler[Load]
  }
  case class Load(project: KappaProject = KappaProject.default) extends ProjectRequest

  object Save {
    implicit val classPickler: Pickler[Save] = boopickle.Default.generatePickler[Save]
  }

  case class Save(project: KappaProject) extends ProjectRequest
  object Create {
    implicit val classPickler: Pickler[Create] = boopickle.Default.generatePickler[Create]
  }
  case class Create(project: KappaProject, rewriteIfExists: Boolean = false) extends ProjectRequest

  object Download {
    implicit val classPickler: Pickler[Download] = boopickle.Default.generatePickler[Download]
  }

  case class Download(projectName: String) extends ProjectRequest

  object Remove {
    implicit val classPickler: Pickler[Remove] = boopickle.Default.generatePickler[Remove]
  }

  case class Remove(projectName: String) extends ProjectRequest
}

object ProjectResponses {

  trait ProjectResponse extends KappaFileMessage

  implicit val pickler = compositePickler[ProjectResponse]
    .addConcreteType[ProjectList]
    .addConcreteType[LoadedProject]

  object ProjectList {
    implicit val classPickler: Pickler[ProjectList] = boopickle.Default.generatePickler[ProjectList]

    lazy val empty = ProjectList(Nil)
  }

  case class ProjectList(projects: List[KappaProject]) extends ProjectResponse

  object LoadedProject {
    implicit val classPickler: Pickler[LoadedProject] = boopickle.Default.generatePickler[LoadedProject]

    lazy val empty = LoadedProject (KappaProject.default)
  }

  case class LoadedProject(project: KappaProject) extends ProjectResponse
}