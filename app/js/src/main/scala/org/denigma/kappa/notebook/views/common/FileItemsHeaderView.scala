package org.denigma.kappa.notebook.views.common

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionSeqView, CollectionSortedSetView}
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.actions.Commands
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable.SortedSet


object FileTabHeaders {
  implicit val getId: String => String = id => id

  def path2name(path: String) = path.lastIndexOf("/") match {
    case -1 => path
    case ind if ind == path.length -1 => path
    case ind => path.substring(ind+1)
  }
}

class FileTabHeaders(val elem: Element, val items: Rx[List[String]], val input: Var[KappaMessage], val selected: Var[String])
                    (implicit getCaption: String => String) extends CollectionSeqView {

  override type Item =  String

  override type ItemView = FileTabItemView

  override def newItemView(item: Item): ItemView= constructItemView(item){
    case (el, _) => new FileTabItemView(el, item, input, selected)(getCaption).withBinder(new GeneralBinder(_))
  }
}

class FileTabItemView(val elem: Element, itemId: String,  val input: Var[KappaMessage], val selected: Var[String] )(implicit getCaption: String => String) extends BindableView {

  val caption: Var[String] = Var(getCaption(itemId))

  val active: rx.Rx[Boolean] = selected.map(value => value == itemId)

  val select = Var(Events.createMouseEvent())
  select.triggerLater({
    selected() = itemId
  })

  val src = Var(if(itemId.contains(":")) itemId else "/files/"+itemId)

  val closeClick = Var(Events.createMouseEvent())
  closeClick.onChange{
    ev=> input() = Commands.CloseFile(itemId)
  }
}
