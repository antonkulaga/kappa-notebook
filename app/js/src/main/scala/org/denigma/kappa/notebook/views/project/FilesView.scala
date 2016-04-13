package org.denigma.kappa.notebook.views.project

import org.denigma.binding.views.{BindableView, ItemsSeqView, ItemsSetView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaPath
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner
import rx.Rx.Dynamic

import scala.collection.immutable.SortedSet


//class ProjectView(elem: Element, root: Var[])

class FilesView(val elem: Element, path: Rx[KappaPath]) extends BindableView with ItemsSeqView {

  override type Item = Var[KappaPath]

  override type ItemView = FilesView

  protected def wrap(p: KappaPath)(implicit ctx: Ctx.Owner): Item = Var.apply(p)
  val items: Rx[List[Item]] = Rx.unsafe{
    val p = path()
    (for(child <- p.children) yield wrap(p))
  }

  override def newItemView(item: Item): FilesView = this.constructItemView(item){
    case (el, params) => new FilesView(el, item).withBinder(v => new CodeBinder(v))
  }

}
