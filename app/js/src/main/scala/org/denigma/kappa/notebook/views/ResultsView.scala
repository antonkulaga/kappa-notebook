package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{GeneralBinder, ReactiveBinder}
import org.denigma.binding.views._
import org.denigma.controls.charts.Series
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.tabs._
import org.denigma.kappa.notebook.KappaHub
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe


/**
  * Created by antonkulaga on 12/5/15.
  */
class ResultsView(val elem: Element, hub: KappaHub) extends BindableView {

  self =>

  protected def defaultContent = ""
  protected def defaultLabel = ""

  type Item = Rx[TabItem]

  val selected: Var[String] = Var("Console")

  override lazy val injector = defaultInjector
    .register("Plots") {
      case (el, params) =>
        new PlotsView(el, selected, hub).withBinder(new CodeBinder(_))
    }
    .register("Console") {
      case (el, params) =>
        new ConsoleView(el, hub.console, selected).withBinder(new CodeBinder(_))
    }
}




