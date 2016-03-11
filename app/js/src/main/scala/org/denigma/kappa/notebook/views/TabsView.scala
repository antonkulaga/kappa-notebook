package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{GeneralBinder, ReactiveBinder}
import org.denigma.binding.views._
import org.denigma.controls.charts.Series
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.tabs._
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.views.charts.PlotsView
import org.denigma.kappa.notebook.views.editor.{EditorUpdates, SBOLEditor}
import org.denigma.kappa.notebook.views.papers.PapersView
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe


class TabsView(val elem: Element, hub: KappaHub) extends BindableView {

  self =>

  protected def defaultContent = ""
  protected def defaultLabel = ""

  type Item = Rx[TabItem]

  val selected: Var[String] = Var("Plots")

  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  override lazy val injector = defaultInjector
    .register("Plots") {
      case (el, params) =>
        new PlotsView(el, selected, hub).withBinder(new CodeBinder(_))
    }
    .register("Console") {
      case (el, params) =>
        new ConsoleView(el, hub.console, selected).withBinder(new CodeBinder(_))
    }
    .register("Papers") {
      case (el, params) =>
        new PapersView(el, selected, hub).withBinder(new CodeBinder(_))
    }
    .register("SBOLEditor") {
      case (el, params) =>
        new SBOLEditor(el, hub, selected, editorsUpdates).withBinder(new CodeBinder(_))
    }
}




