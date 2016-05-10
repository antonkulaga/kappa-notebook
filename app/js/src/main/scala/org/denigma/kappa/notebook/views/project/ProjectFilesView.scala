package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, ItemsSeqView, ItemsSetView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages._
import org.scalajs.dom._
import org.scalajs.dom.raw.Element
import rx._
import org.denigma.binding.extensions._
import scala.collection.immutable._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic

class ProjectFilesView(val elem: Element, val currentProject: Rx[KappaProject]) extends ItemsSetView {

  val items: Rx[SortedSet[KappaFile]] = currentProject.map(proj => proj.folder.files)

  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectFileView(el,  item).withBinder(v=>new GeneralBinder(v))
  }

  override type Item = KappaFile
  override type ItemView = ProjectFileView

  val name = currentProject.map(proj => proj.name)
  val save = Var(Events.createMouseEvent())
  save.onChange{
    case ev =>
      //connector.send(Save(currentProject.now))
  }

  val download: Var[MouseEvent] = Var(Events.createMouseEvent())
  download.onChange{
    case ev =>
  }


}

class ProjectFileView(val elem: Element, val file: KappaFile) extends BindableView {

  val name = Var(file.name)
  val icon: Rx[String] = name.map{
    case n if n.endsWith(".pdf") => "File Pdf Outline" + " large icon"
    case n if n.endsWith(".txt") => "File Text Outline" + " large icon"
    case n if n.endsWith(".ka") | n.endsWith(".ttl") => "File Code Outline" + " large icon"
    case n if n.endsWith(".svg") | n.endsWith(".png") | n.endsWith(".jpg") | n.endsWith(".gif") => "File Image Outline" + " large icon"
    case n if n.endsWith(".avi") => "File Video Outline" + " large icon"
    case other => "File Outline"
  }

}