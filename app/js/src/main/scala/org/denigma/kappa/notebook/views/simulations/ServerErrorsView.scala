package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, CollectionSeqView}
import org.scalajs.dom.raw.Element
import rx.{Rx, Var}
import rx.Ctx.Owner.Unsafe.Unsafe

object ServerErrorsView {

  class ServerErrorView(val elem: Element, error: String) extends BindableView{
    val message = Var(error)
  }
}
class ServerErrorsView(val elem: Element, val items: Rx[List[String]]) extends BindableView with CollectionSeqView {
  import ServerErrorsView._

  type Item = String

  type ItemView = ServerErrorView

  val hasErrors = items.map(its=>its.nonEmpty)

  override def newItemView(item: String): ServerErrorView = this.constructItemView(item){
    case (el, args) => new ServerErrorView(el, item).withBinder(v=> new GeneralBinder(v))
  }
}
