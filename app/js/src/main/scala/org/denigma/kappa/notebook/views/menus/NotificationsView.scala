package org.denigma.kappa.notebook.views.menus

import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages.{KappaMessage, ServerErrors}
import org.scalajs.dom.raw.Element
import rx._
import org.denigma.binding.extensions._
import org.scalajs.dom
import rx.Ctx.Owner.Unsafe.Unsafe
/**
  * Created by antonkulaga on 7/29/16.
  */
class NotificationsView(val elem: Element, val input: Rx[KappaMessage]) extends BindableView{
  val message: Var[String] = Var("")
  val hasMessage = message.map(m=>m!="")
  input.onChange{
    case ServerErrors(errors) =>
      val ers = errors.foldLeft(""){ case (acc, e) => acc +" "+e}
      dom.console.error("Server errors: "+ers)
      message() = ers
    case _ => //do nothing
  }

}
