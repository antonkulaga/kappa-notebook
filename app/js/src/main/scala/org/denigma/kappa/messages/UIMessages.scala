package org.denigma.kappa.messages

import org.denigma.controls.papers.Bookmark
import rx.Rx.Dynamic
import rx.{Rx, Var}

import scala.collection.immutable.SortedSet


object GoToPaper {
  /*
    implicit val textLayerSelectionPickler = boopickle.Default.generatePickler[TextLayerSelection]

    implicit val bookmarkPickler = boopickle.Default.generatePickler[Bookmark]

    implicit val goToPickler = boopickle.Default.generatePickler[GoToPaper]
    */
}

case class GoToPaper (bookmark: Bookmark) extends UIMessage

object CurrentProject {

  protected implicit def toMap(st: SortedSet[KappaFile]): Map[String, KappaFile] = st.map(f=> (f.name, f)).toMap

  def fromKappaProject(kappaProject: KappaProject) = {
    CurrentProject(kappaProject.name, kappaProject.folder.path, kappaProject.sourceMap, kappaProject.papers, kappaProject.images, kappaProject.videos, kappaProject.otherFiles, kappaProject.saved)
  }
}

case class CurrentProject(name: String,
                          path: String,
                          sourceMap: Map[String, KappaFile],
                          papers: Map[String, KappaFile],
                          images: Map[String, KappaFile],
                          videos: Map[String, KappaFile],
                          otherFiles: Map[String, KappaFile],
                          saved: Boolean
                         )
{
  lazy val allFilesMap: Map[String, KappaFile] = sourceMap ++ papers ++ images ++ videos ++  otherFiles
  lazy val allFiles = SortedSet(allFilesMap.values.toSeq:_*)

  def toKappaProject: KappaProject = KappaProject(name, KappaFolder(path, SortedSet.empty, allFiles), saved)
}

/*
object CurrentProject {

  def fromKappaProject(kappaProject: KappaProject) = CurrentProject(
    Var(kappaProject.name),
    Var(kappaProject.sourceMap),
    Var(kappaProject.papers),
    Var(kappaProject.images),
    Var(kappaProject.videos),
    Var(kappaProject.otherFiles)
  )

  lazy val empty = CurrentProject(Var(Map.empty), Var(SortedSet.empty), Var(SortedSet.empty), Var(SortedSet.empty), Var(SortedSet.empty),)

}

/**
  * Just a class, probably temporal to keep
  * @param name
  * @param sourceMap
  * @param papers
  * @param images
  * @param videos
  * @param otherFiles
  */
case class CurrentProject(
                          name: Var[String],
                          sourceMap: Var[Map[String, KappaFile]],
                          papers: Var[SortedSet[KappaFile]],
                          images: Var[SortedSet[KappaFile]],
                          videos: Var[SortedSet[KappaFile]],
                          otherFiles: Var[SortedSet[KappaFile]]
                         ) extends UIMessage {

  lazy val sources: Dynamic[SortedSet[KappaFile]] = sourceMap.map(v=>SortedSet.apply(v.values.toSeq:_*))

  val allFiles = Rx{
    sources() ++ papers() ++ images() ++ videos() ++ otherFiles()
  }

  val dirty = allFiles.map()
}
*/

