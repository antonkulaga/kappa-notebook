package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{Events, GeneralBinder, ReactiveBinder}
import org.denigma.binding.views._
import org.denigma.controls.charts.{Point, StaticSeries, LineStyles}
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
        val justSomeLines =
          Var(
            Seq(Var
            (new StaticSeries("Points: [1, 1] , [2, 3], [3 ,1], [4, 3]", List(
              Point(1.0, 1.0),
              Point(2.0, 3.0),
              Point(3.0, 1.0),
              Point(4.0, 3.0)),
              LineStyles.default.copy(strokeColor = "blue")
            ))))
        new Charts(el, justSomeLines, selected).withBinder(new AdvancedBinder(_, self.binders.collectFirst { case r: ReactiveBinder => r }))
    }
    .register("Console") {
      case (el, params) =>
        new ConsoleView(el, hub.console, selected).withBinder(new AdvancedBinder(_))
    }
}



class ConsoleView(val elem: Element, kappaConsole: Rx[Option[KappaMessages.Console]], val selected: Var[String]) extends BindableView{
  val console: Rx[String] = kappaConsole.map(_.map(c=>c.text).getOrElse(""))
  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)
}
