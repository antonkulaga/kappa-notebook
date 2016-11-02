package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.views._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._


/**
  * View to show WebSim console output
  */
class ConsoleView(val elem: Element, val log: Rx[List[String]], val selected: Var[String]) extends BindableView
{

  val active: Rx[Boolean] = selected.map(s=>s=="console")

  val console: Rx[String] = log.map(list => list.mkString("\n"))
}
