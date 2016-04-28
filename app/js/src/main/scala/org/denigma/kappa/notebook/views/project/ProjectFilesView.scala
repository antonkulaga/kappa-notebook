package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsSeqView, ItemsSetView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.{KappaFile, KappaFolder, KappaPath, KappaProject}
import org.denigma.kappa.notebook.KappaHub
import org.scalajs.dom.raw.Element
import rx._

import scala.collection.immutable._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic

class ProjectFilesView(val elem: Element, hub: KappaHub) extends ItemsSetView {

  val items: Rx[SortedSet[KappaFile]] = hub.loaded.map(l=>l.project.folder.files)


  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectFileView(el,  item).withBinder(v=>new GeneralBinder(v))

  }

  override type Item = KappaFile
  override type ItemView = ProjectFileView

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