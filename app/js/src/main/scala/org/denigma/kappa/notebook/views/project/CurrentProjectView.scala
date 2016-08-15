package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.CollectionSortedSetView
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.actions.Commands
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.{BlobPropertyBag, Element}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.denigma.kappa.notebook.extensions._

import scala.collection.immutable._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.typedarray.{TypedArrayBuffer, Uint8Array}
import scala.util.{Failure, Success}


class CurrentProjectView(val elem: Element,
                         val currentProject: Var[KappaProject],
                         //val sourceMap: Var[Map[String, KappaSourceFile]],
                         input: Var[KappaMessage],
                         output: Var[KappaMessage],
                         uploadId: String
                        ) extends CollectionSortedSetView {

  val projectName = currentProject.map(_.name)

  lazy val uploadInput = sq.byId(uploadId).get.asInstanceOf[Input]

  override type Item = KappaFile

  override type ItemView = ProjectFileView

  val newFileName: Var[String] = Var("")

  val hasNewFile: Rx[Boolean] = newFileName.map(f=>f.length > 1)

  val fileName: Rx[String] = newFileName.map{
    case f if f.endsWith(".ka")=>f
    case other => other
  }

  val canCreate: Rx[Boolean] = Rx{
    val proj = currentProject()
    val path = proj + "/" + fileName()
    !currentProject().folder.files.exists(f => path ==f.path)
  }
  val unsaved: Rx[Map[String, KappaFile]] = currentProject.map(p=>p.folder.allFiles.filterNot(f=>f.saved).map(f=> f.path -> f).toMap)

  val hasUnsaved: Rx[Boolean] = unsaved.map(u=>u.nonEmpty)

  val addFile = Var(Events.createMouseEvent())
  addFile.triggerIf(canCreate){ ev=>
    val file = KappaSourceFile(projectName.now + "/" + newFileName.now, "", saved = false)
    val saveRequest = FileRequests.Save(List(file), rewrite = false, getSaved = true)
    output() = saveRequest
    //output() = //org.denigma.kappa.messages.FileRequests.Save(projectName.now, List())
  }

  val uploadFile = Var(Events.createMouseEvent())
  uploadFile.triggerLater{
       uploadInput.click()
  }

  val saveAll = Var(Events.createMouseEvent())
  saveAll.triggerLater{
    saveAllHandler()
   }

  protected def saveAllHandler() = {
    val toSave = unsaved.now.values.toList
    if(toSave.nonEmpty) {
      val saveRequest = FileRequests.Save(toSave, rewrite = true)
      //println("save ALL!")
      output() = saveRequest
    }
  }

  val items: Rx[SortedSet[KappaFile]] = currentProject.map(proj => proj.folder.files)

  protected def inProject(str: String) = str.startsWith(projectName.now)|| str.startsWith("/"+projectName.now)

  input.onChange{
    case d @ FileResponses.Downloaded(label, data)=>
      val name = if(!label.contains(".")) label+".zip" else label
      //println("DOWNLOADED IS = "+d)
      if(d != FileResponses.Downloaded.empty) saveBinaryAs(name, data)

    case b @ KappaBinaryFile(path, buff, _, _) if currentProject.now.folder.allFilesMap.contains(path) =>
      //currentProject.now = currentProject.now.copy(folder = )
      dom.console.log("updating binary parts is not yet implemented")

    case Done(FileRequests.Remove(pathes), _)  =>
      val (in, not) = pathes.partition(inProject)
      if(in.nonEmpty){
        currentProject() = currentProject.now.copy(folder = currentProject.now.folder.removeFiles(pathes))
      }

    case resp @ FileResponses.RenamingResult(renamed: Map[String, (String, String)], nameConflicts, notFound)=>
      dom.console.error("RENAMING IS NOT YET IMPLEMENTED IN UI")

    case resp @ FileResponses.SavedFiles(Left(pathes)) =>
      val proj = currentProject.now

      currentProject() = { proj.copy(folder = proj.folder.markSaved(pathes)) }
      //println("save resp")
      //pprint.pprintln(resp)

    case resp @ FileResponses.SavedFiles(Right(files)) =>
      val proj = currentProject.now
      currentProject() = proj.copy(folder = proj.folder.addFiles(files))

    case Commands.SaveAll =>
      saveAllHandler()

    case _=> //do nothing
  }

  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectFileView(el, item, input, output).withBinder(v => new GeneralBinder(v))
  }

  val name = currentProject.map(proj => proj.name)
  val save = Var(Events.createMouseEvent())
  save.onChange{ ev =>
      output() = ProjectRequests.Save(currentProject.now)
      //connector.send(Save(currentProject.now))
  }

  val download: Var[MouseEvent] = Var(Events.createMouseEvent())
  download.triggerLater{
      val mes = ProjectRequests.Download(currentProject.now.name)
      //println("download ="+mes)
      output() = mes
  }
  import js.JSConverters._

  override def subscribeUpdates() = {
    super.subscribeUpdates()
    uploadInput.addEventListener[Event](Events.change, filesHandler _)
  }
  protected def filesHandler(event: org.scalajs.dom.Event) = {
    val name = fileName.now
    dom.console.info("FILE UPLOAD WORKS!")
    val files = uploadInput.files.toList
    dom.console.info("FIELS ARE :"+files)


    files.foreach{ f =>
      f.readAsByteBuffer.onComplete{
          case Failure(th) => dom.console.error("file upload failed with: "+th)
          case Success(result) =>
            //val mess = DataMessage(f.name, result)
            //println(s"uploading ${f.name} to ${projectName.now}")
            val fl = KappaBinaryFile(projectName.now+"/"+f.name, result)
            output() = FileRequests.Save(List(fl), rewrite = true, getSaved = true)
        }
    }
    //val toUplod = FileRequests.UploadBinary()
    //val k = KappaFile("", name, "", saved = false)
    //val uploadRequest = FileRequests.Save(projectName.now, List(k), rewrite = false, getSaved = true)
    //output() = uploadRequest
  }

  def saveBinaryAs(filename: String, data: Array[Byte]) = {
    //println(s"binary for $filename")
    val pom = dom.document.createElement("a")
    pom.setAttribute("id","pom")
    val options = BlobPropertyBag("octet/stream")
    val arr = new Uint8Array(data.toJSArray)
    val blob = new Blob(js.Array(arr), options)
    //val url = dom.window.dyn.URL.createObjectURL(blob)
    val reader = new FileReader()
    def onLoadEnd(ev: ProgressEvent): Any = {
      val url = reader.result
      pom.asInstanceOf[dom.raw.HTMLAnchorElement].href = url.asInstanceOf[String]
      pom.setAttribute("download", filename)
      pom.dyn.click()
      if(pom.parentNode==dom.document) dom.document.removeChild(pom)
    }
    reader.onloadend = onLoadEnd _
    reader.readAsDataURL(blob)
  }

}


