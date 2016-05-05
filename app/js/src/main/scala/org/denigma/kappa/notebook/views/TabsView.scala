package org.denigma.kappa.notebook.views

import org.denigma.binding.views._
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.views.editor.EditorUpdates
import org.denigma.kappa.notebook.views.papers.PapersView
import org.denigma.kappa.notebook.views.simulations.SimulationsView
import org.scalajs.dom.raw.Element
import rx._


class TabsView(val elem: Element, hub: KappaHub) extends BindableView {

  self =>

  lazy val selected = hub.selector.tab

  protected def defaultContent = ""
  protected def defaultLabel = ""

  type Item = Rx[common.TabItem]



  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  override lazy val injector = defaultInjector
    .register("Simulations") {
      case (el, params) =>
        new SimulationsView(el, selected, hub.simulations, hub.launcher).withBinder(new CodeBinder(_))
    }
    /*
    .register("Console") {
      case (el, params) =>
        new ConsoleView(el, hub.console, selected).withBinder(new CodeBinder(_))
    }
    */

    .register("Image") {
      case (el, params) =>
        new ImageView(el, selected, hub.selector.image).withBinder(new CodeBinder(_))
    }

    .register("Papers") {
      case (el, params) =>
        new PapersView(el, selected, hub.papers, hub.selector, hub.kappaCursor).withBinder(new CodeBinder(_))
    }
    .register("UnderDevelopment") {
      case (el, params) =>
        el.id = "UnderDevelopment" //dirty trick to set viewid
        new UnderDevelopment(el, selected).withBinder(new CodeBinder(_))
    }
    /*
    .register("SBOLEditor") {
      case (el, params) =>
        new SBOLEditor(el, hub, selected, editorsUpdates).withBinder(new CodeBinder(_))
    }
    */
}
class UnderDevelopment(val elem: Element, val selected: Var[String]) extends BindableView with common.TabItem {

}