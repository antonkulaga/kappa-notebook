package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.views._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._


/**
  * Created by antonkulaga on 12/31/15.
  */
class ConsoleView(val elem: Element, val console: Rx[String], val selected: Var[String]) extends BindableView
{
  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)
}
