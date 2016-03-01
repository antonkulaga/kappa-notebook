package org.denigma.kappa.notebook.views
import org.denigma.binding.views._
import org.denigma.kappa.messages.KappaMessages
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe


/**
  * Created by antonkulaga on 12/31/15.
  */
class ConsoleView(val elem: Element, kappaConsole: Rx[KappaMessages.Console], val selected: Var[String]) extends BindableView{

  val console: Rx[String] = kappaConsole.map(_.text)
  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)
}
