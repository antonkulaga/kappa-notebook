package org.denigma.kappa.notebook.views.visual

import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages.{ServerMessages, KappaMessage}
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName


@js.native
@JSName("ContactMap")
class ContactMapRenderer(val id: String, isSnapshot: Boolean) extends js.Object {

  def setData(data: Any): Unit = js.native

  def clearData(): Unit = js.native

  def exportJSON: Any = js.native

}


class ContactMapView(val elem: Element, val input: Var[KappaMessage]) extends BindableView {

  input.onChange{

    case KappaMessage.ServerResponse(ServerMessages.ParseResult(_, contactMap))=>

    //case KappaMessage.ServerResponse(ServerMessages.LaunchModel) =>
    case _ => //do nothing

  }

  lazy val renderer = new ContactMapRenderer(elem.id, false)


  override def bindView() = {
    super.bindView()
    println("contact map initialized!!!")
  }

}
