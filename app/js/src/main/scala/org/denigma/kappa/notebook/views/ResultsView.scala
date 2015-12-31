package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{Events, GeneralBinder, ReactiveBinder}
import org.denigma.binding.views._
import org.denigma.controls.charts.{Series, Point, StaticSeries, LineStyles}
import org.denigma.controls.tabs._
import org.denigma.kappa.messages.KappaMessages
import org.denigma.kappa.notebook.KappaHub
import org.scalajs.dom.raw.Element
import rx.core._

import scala.collection.immutable._


import rx.ops._

/**
  * Created by antonkulaga on 12/5/15.
  */
class ResultsView(val elem: Element, hub: KappaHub) extends BindableView {

  self =>

  protected def defaultContent = ""
  protected def defaultLabel = ""

  type Item = Rx[TabItem]

  val selected: Var[String] = Var("Console")

  binders = List(new BinderForViews2[this.type](this))


  override lazy val injector = defaultInjector
    .register("Chart") {
      case (el, params) =>
        val items: rx.Rx[scala.collection.immutable.Seq[Rx[Series]]] =  hub.chart.map(chart=>chart.series.map(s=>Var(s)))
        new ChartView(el, items, selected).withBinder(new AdvancedBinder(_, self.binders.collectFirst { case r: ReactiveBinder => r }))
    }
    .register("Console") {
      case (el, params) =>
        new ConsoleView(el, hub.console, selected).withBinder(new AdvancedBinder(_))
    }
}




