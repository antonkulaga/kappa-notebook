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
import org.scalajs.dom.raw.{BlobPropertyBag, Element}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable._
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

class CurrentProjectView(val elem: Element, currentProject: Rx[CurrentProject],  input: Var[KappaMessage], output: Var[KappaMessage]) extends ItemsSetView {

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

  val unsaved: Dynamic[Map[String, KappaFile]] = sourceMap.map{ sm=> sm.collect{ case (key, value) if !value.saved => key -> value } }

  val addFile = Var(Events.createMouseEvent())
  addFile.triggerIf(canCreate) { case ev=>
    val file = KappaFile("", newFileName.now, "", false)
    output() = FileRequests.Save(projectName.now, List(file), false)
    //output() = //org.denigma.kappa.messages.FileRequests.Save(projectName.now, List())
  }

  val uploadFile = Var(Events.createMouseEvent())
  uploadFile.triggerLater{
    val name = fileName.now
    val k = KappaFile("", name, "", saved = false)
    output() = FileRequests.Save(projectName.now, List(k), false)
  }

  val items: Rx[SortedSet[KappaFile]] = currentProject.map(proj => proj.allFiles)

  input.onChange{
    case d @ FileResponses.Downloaded(label, data)=>
      val name = if(!label.contains(".")) label+".zip" else label
      //println("DOWNLOADED IS = "+d)
      if(d != FileResponses.Downloaded.empty) saveBinaryAs(name, data)

    case _=> //do nothing
  }

  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectFileView(el, item, name, input, output).withBinder(v => new GeneralBinder(v))
  }

  override type Item = KappaFile
  override type ItemView = ProjectFileView

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

object FileType extends Enumeration {
  type FileType = Value
  val pdf, txt, source, image, video, other = Value
}

class ProjectFileView(val elem: Element, val file: KappaFile, parentName: Rx[String], input: Var[KappaMessage], output: Var[KappaMessage]) extends BindableView {

  val name = Var(file.name)
  val fileType: Rx[FileType.Value] = name.map{
    case n if n.endsWith(".pdf") => FileType.pdf
    case n if n.endsWith(".txt") => FileType.txt
    case n if n.endsWith(".ka") | n.endsWith(".ttl") => FileType.source
    case n if n.endsWith(".svg") | n.endsWith(".png") | n.endsWith(".jpg") | n.endsWith(".gif") => FileType.image
    case n if n.endsWith(".avi") => FileType.video
    case other => FileType.other
  }

  val isSource: Rx[Boolean] = fileType.map(f=>f==FileType.source)
  val isImage: Rx[Boolean] = fileType.map(f=>f==FileType.image)
  val isVideo: Rx[Boolean] = fileType.map(f=>f==FileType.video)
  val isPaper: Rx[Boolean] = fileType.map(f=>f==FileType.pdf)

  val icon: Rx[String] = fileType.map{
    case FileType.pdf => "File Pdf Outline" + " large icon"
    case FileType.txt => "File Text Outline" + " large icon"
    case FileType.source => "File Code Outline" + " large icon"
    case FileType.image => "File Image Outline" + " large icon"
    case FileType.video => "File Video Outline" + " large icon"
    case other => "File Outline"
  }

  val openClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  openClick.triggerLater{
    fileType.now match {
      case FileType.pdf => input() =
        KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Papers))
        .andThen(GoToPaper(Bookmark(file.name, 1)))

      case FileType.source => input() =
        KappaMessage.Container()
          .andThen(Go.ToTab(MainTabs.Editor))
          .andThen(Go.ToSource(filename = file.name))

      case other => //do nothing
    }
  }

  val removeClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  removeClick.triggerLater{
    output() = FileRequests.Remove(parentName.now, file.name)
  }

  val saveClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveClick.triggerLater{
    output() = FileRequests.Save(projectName = name.now, List(file), rewrite = true)
  }

  val renameClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  renameClick.triggerLater{

  }


}