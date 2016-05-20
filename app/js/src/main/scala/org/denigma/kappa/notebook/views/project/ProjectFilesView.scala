package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsSetView}
import org.denigma.kappa.messages._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.{BlobPropertyBag, Element}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

class ProjectFilesView(val elem: Element, val currentProject: Rx[KappaProject], input: Var[KappaMessage], output: Var[KappaMessage]) extends ItemsSetView {

  val items: Rx[SortedSet[KappaFile]] = currentProject.map(proj => proj.folder.files)


  input.onChange{
    case d @ FileResponses.Downloaded(label, data)=>
      val name = if(!label.contains(".")) label+".zip" else label
      //println("DOWNLOADED IS = "+d)
      if(d != FileResponses.Downloaded.empty) saveBinaryAs(name, data)

    case _=> //do nothing
  }

  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectFileView(el,  item, name, output).withBinder(v => new GeneralBinder(v))
  }

  override type Item = KappaFile
  override type ItemView = ProjectFileView

  val name = currentProject.map(proj => proj.name)
  val save = Var(Events.createMouseEvent())
  save.onChange{
    case ev =>
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

class ProjectFileView(val elem: Element, val file: KappaFile, parentName: Rx[String], output: Var[KappaMessage]) extends BindableView {

  val name = Var(file.name)
  val icon: Rx[String] = name.map{
    case n if n.endsWith(".pdf") => "File Pdf Outline" + " large icon"
    case n if n.endsWith(".txt") => "File Text Outline" + " large icon"
    case n if n.endsWith(".ka") | n.endsWith(".ttl") => "File Code Outline" + " large icon"
    case n if n.endsWith(".svg") | n.endsWith(".png") | n.endsWith(".jpg") | n.endsWith(".gif") => "File Image Outline" + " large icon"
    case n if n.endsWith(".avi") => "File Video Outline" + " large icon"
    case other => "File Outline"
  }

  val removeClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  removeClick.triggerLater{
    output() = FileRequests.Remove(parentName.now, file.name)
  }


}