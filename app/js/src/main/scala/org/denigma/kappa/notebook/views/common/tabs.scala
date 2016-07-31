package org.denigma.kappa.notebook.views.common

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, CollectionSortedSetView}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._

object TabHeaders {
  implicit val getId: String => String = id => id
}

class TabHeaders(val elem: Element, val items: Rx[SortedSet[String]], val selected: Var[String])(implicit getCaption: String => String) extends CollectionSortedSetView {

  override type Item =  String

  override type ItemView = TabHeaderItemView

  override def newItemView(item: Item): ItemView= constructItemView(item){
    case (el, _) => new TabHeaderItemView(el, item,  selected)(getCaption).withBinder(new GeneralBinder(_))
  }
}

class TabHeaderItemView(val elem: Element, itemId: String,  val selected: Var[String] )(implicit getCaption: String => String) extends BindableView {

  val caption: Var[String] = Var(getCaption(itemId))

  val active: rx.Rx[Boolean] = selected.map(value => value == itemId)

  val select = Var(Events.createMouseEvent())
  select.triggerLater({
    selected() = itemId
    }
  )
}
