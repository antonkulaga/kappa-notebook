package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsSetView}
import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.MainTabs
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.{BlobPropertyBag, Element}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable._
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import org.scalajs.dom.ext._


class CurrentProjectView(val elem: Element,
                         currentProject: Var[CurrentProject],
                         input: Var[KappaMessage],
                         output: Var[KappaMessage],
                         uploadId: String
                        ) extends ItemsSetView {


  //lazy val uploadInput = sq.byId(uploadId).collect{ case i: Input => i}.get

  override type Item = KappaFile

  override type ItemView = ProjectFileView

  val sourceMap = currentProject.map(p=>p.sourceMap)

  val projectName: Rx[String] = currentProject.map(p=>p.name)

  val newFileName: Var[String] = Var("")

  val hasNewFile: Rx[Boolean] = newFileName.map(f=>f.length > 1)

  val fileName: Rx[String] = newFileName.map{
    case f if f.endsWith(".ka")=>f
    case other => other
  }

  val canCreate: Rx[Boolean] = Rx{
    val sources = sourceMap()
    val name = fileName()
    !sources.contains(name)
  }
  val unsaved: Rx[Map[String, KappaFile]] = sourceMap.map{ sm=> sm.collect{ case (key, value) if !value.saved => key -> value } }

  val hasUnsaved: Rx[Boolean] = unsaved.map(u=>u.nonEmpty)

  val addFile = Var(Events.createMouseEvent())
  addFile.triggerIf(canCreate){ case ev=>
    val file = KappaFile("", newFileName.now, "", saved = false)
    val saveRequest = FileRequests.Save(projectName.now, List(file), rewrite = false, getSaved = true)
    output() = saveRequest
    //output() = //org.denigma.kappa.messages.FileRequests.Save(projectName.now, List())
  }

  val uploadFile = Var(Events.createMouseEvent())
  uploadFile.triggerLater{
       // uploadInput.click()
  }

  val saveAll = Var(Events.createMouseEvent())
  saveAll.triggerLater{
    val toSave = unsaved.now.values.toList
    val saveRequest = FileRequests.Save(projectName = projectName.now, toSave, rewrite = true)
    println("save ALL!")
    output() = saveRequest
  }

  val items: Rx[SortedSet[KappaFile]] = currentProject.map(proj => proj.allFiles)

  input.onChange{
    case d @ FileResponses.Downloaded(label, data)=>
      val name = if(!label.contains(".")) label+".zip" else label
      //println("DOWNLOADED IS = "+d)
      if(d != FileResponses.Downloaded.empty) saveBinaryAs(name, data)

    case Done(FileRequests.Remove(pname, filename), _) if pname == currentProject.now.name =>
      currentProject() = currentProject.now.removeByName(filename)

    case resp @ FileResponses.RenamingResult(pname, renamed: Map[String, (String, String)], nameConflicts, notFound) if pname == currentProject.now.name =>
      currentProject() = currentProject.now.withRenames(renamed)

    case resp @ FileResponses.SavedFiles(pname, Left(names)) if pname == currentProject.now.name =>
      val proj = currentProject.now
      currentProject() = { proj.markSaved(names) }
      //println("save resp")
      //pprint.pprintln(resp)

    case resp @ FileResponses.SavedFiles(pname, Right(files)) if pname == currentProject.now.name =>
      val proj = currentProject.now
      currentProject() = {
        //println("mark saved with addition!")
        //pprint.pprintln(resp)
        proj.updateWithSaved(files)
      }

    case _=> //do nothing
  }

  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectFileView(el, item, name, input, output).withBinder(v => new GeneralBinder(v))
  }

  val name = currentProject.map(proj => proj.name)
  val save = Var(Events.createMouseEvent())
  save.onChange{
    case ev =>
      output() = ProjectRequests.Save(currentProject.now.toKappaProject)
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
    //uploadInput.addEventListener[Event](Events.change, filesHandler _)
  }
  protected def filesHandler(event: org.scalajs.dom.Event) = {
    println("on change files work")
    val name = fileName.now

    /*
    val files = uploadInput.files.toList.foreach{
      case f =>
        /*
        f.readAsArrayBuffer.onSuccess{
          case result =>
            println("byte buffer readinf finished")
        }
        */
    }
*/
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


