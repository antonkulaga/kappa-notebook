package org.denigma.kappa.notebook.views.common

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, ItemsSetView}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._

class TabHeaders(val elem: Element, val items: Rx[SortedSet[String]], val selected: Var[String]) extends ItemsSetView {


  override type Item =  String

  override type ItemView = TabHeaderItemView

  override def newItemView(item: Item): TabHeaderItemView = constructItemView(item){
    case (el, _) => new TabHeaderItemView(el, item,  selected).withBinder(new GeneralBinder(_))
  }
}

class TabHeaderItemView(val elem: Element, viewId: String,  val selected: Var[String] ) extends BindableView {

  val caption: Var[String] = Var(viewId)

  val active: rx.Rx[Boolean] = selected.map(value => value == viewId)

  val select = Var(Events.createMouseEvent())
  select.triggerLater({
    selected() = viewId
    }
  )
}
