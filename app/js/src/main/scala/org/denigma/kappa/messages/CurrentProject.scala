package org.denigma.kappa.messages

import scala.collection.immutable.SortedSet


object CurrentProject extends FileFilters {

  protected implicit def toMap(st: SortedSet[KappaFile]): Map[String, KappaFile] = st.map(f=> (f.name, f)).toMap

  def apply(name: String, place: String, allFiles: List[Map[String, KappaFile]] ): CurrentProject = {
    CurrentProject(name, place, allFiles.head,  allFiles(1),  allFiles(2),  allFiles(3),  allFiles(4))
  }

  def fromKappaProject(kappaProject: KappaProject) = {
    CurrentProject(kappaProject.name,
      kappaProject.folder.path,
      kappaProject.sourceMap,
      kappaProject.papers,
      kappaProject.images,
      kappaProject.videos,
      kappaProject.otherFiles)
  }

  def fromMap(name: String, path: String, files: Map[String, KappaFile]) = {
    val ((sources, papers, images, videos, otherFiles)) = fileMapsByType(files)
    CurrentProject(name,
      path,
      sources,
      papers,
      images,
      videos,
      otherFiles)
  }

  def filesByType(files: List[KappaFile]): (List[KappaFile], List[KappaFile], List[KappaFile], List[KappaFile], List[KappaFile]) = {
    files.foldLeft(List.empty[KappaFile], List.empty[KappaFile], List.empty[KappaFile], List.empty[KappaFile], List.empty[KappaFile]){
      case ((sources, papers, images, videos, otherFiles), file) if sourceFilter(file)=> (file::sources, papers, images, videos, otherFiles)
      case ((sources, papers, images, videos, otherFiles), file) if paperFilter(file)=> (sources, file::papers, images, videos, otherFiles)
      case ((sources, papers, images, videos, otherFiles), file) if imageFilter(file)=> (sources, papers, file::images, videos, otherFiles)
      case ((sources, papers, images, videos, otherFiles), file) if videoFilter(file)=> (sources, papers, images, file::videos, otherFiles)
      case ((sources, papers, images, videos, otherFiles), file) => (sources, papers, images, videos, file::otherFiles)
    }
  }

  def fileMapsByType(files: Map[String, KappaFile]) = {
    files.foldLeft(Map.empty[String, KappaFile], Map.empty[String, KappaFile], Map.empty[String, KappaFile], Map.empty[String, KappaFile], Map.empty[String, KappaFile]){
      case ((sources, papers, images, videos, otherFiles), (name , file)) if sourceFilter(file)=> (sources updated  (name , file), papers, images, videos, otherFiles)
      case ((sources, papers, images, videos, otherFiles), (name , file)) if paperFilter(file)=> (sources, papers updated  (name , file), images, videos, otherFiles)
      case ((sources, papers, images, videos, otherFiles), (name , file)) if imageFilter(file)=> (sources, papers, images updated  (name , file), videos, otherFiles)
      case ((sources, papers, images, videos, otherFiles), (name , file)) if videoFilter(file)=> (sources, papers, images, videos updated (name , file), otherFiles)
      case ((sources, papers, images, videos, otherFiles), (name , file)) => (sources, papers, images, videos, otherFiles updated (name , file))
    }
  }
}

case class CurrentProject(name: String,
                          path: String,
                          sourceMap: Map[String, KappaFile],
                          papers: Map[String, KappaFile],
                          images: Map[String, KappaFile],
                          videos: Map[String, KappaFile],
                          otherFiles: Map[String, KappaFile]
                         ) extends FileFilters
{
  self=>

  def -(proj: CurrentProject) = {
    this.copy(
      sourceMap = self.sourceMap -- proj.sourceMap.keys,
      papers = self.papers -- proj.papers.keys,
      images = self.images -- proj.images.keys,
      videos = self.videos -- proj.videos.keys,
      otherFiles = self.otherFiles -- proj.otherFiles.keys
    )
  }

  def +(proj: CurrentProject) = {
    this.copy(
      sourceMap = self.sourceMap ++ proj.sourceMap,
      papers = self.papers ++ proj.papers,
      images = self.images ++ proj.images,
      videos = self.videos ++ proj.videos,
      otherFiles = self.otherFiles ++ proj.otherFiles
    )
  }

  lazy val allFilesList: List[Map[String, KappaFile]] = List(sourceMap, papers, images, videos, otherFiles)

  lazy val allFilesMap: Map[String, KappaFile] = sourceMap ++ papers ++ images ++ videos ++  otherFiles

  lazy val allFiles = SortedSet(allFilesMap.values.toSeq:_*)

  def toKappaProject: KappaProject = KappaProject(name, KappaFolder(path, SortedSet.empty, allFiles))

  def removeByName(name: String): CurrentProject = {
    if(sourceMap.contains(name)) this.copy(sourceMap = sourceMap - name) else
    if(papers.contains(name)) this.copy(papers = papers - name) else
    if(images.contains(name)) this.copy(images = images - name) else
    if(videos.contains(name)) this.copy(videos = videos - name) else
      this.copy(otherFiles = otherFiles - name)
  }

  protected def update(fun: Map[String, KappaFile] =>  Map[String, KappaFile] ) = {
    val updated = for(mp <- allFilesList) yield fun(mp)
    CurrentProject.apply(name, path, updated)
  }

  def markSaved(fileNames: Set[String]): CurrentProject = this.update{
    case mp =>  mp.map{ case (key, value)=> if(fileNames.contains(key)) key->value.copy(saved = true) else key-> value}
  }

  def updateWithSaved(saved: Map[String, KappaFile]): CurrentProject =
  {
    val keys = saved.keySet
    val (sources, papers, images, videos, otherFiles)  = CurrentProject.fileMapsByType(saved)
    this.copy(sourceMap = self.sourceMap ++ sources,
      papers = self.papers ++ papers,
      images = self.images ++ images,
      videos = self.videos ++ videos,
      otherFiles = self.otherFiles ++ otherFiles
    )
  }
  /*this.update{
    case mp =>  mp.map{
      case (key, value)=> if(files.contains(key)) key-> files(key) else key-> value}
  }
  */

  def withRenames(renames: Map[String, (String, String)]) =  this.update{
    case mp =>  mp.map{
      case (key, value) if renames.contains(key)=>
        val (newName, newPath) = renames(key)
        newName -> value.copy(path = newPath)
      case (key, value)=> key -> value
    }
  }
}