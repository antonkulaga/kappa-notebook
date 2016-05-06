package org.denigma.kappa.notebook.views

import org.denigma.binding.views._
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.Selector
import org.denigma.kappa.notebook.views.editor.EditorUpdates
import org.denigma.kappa.notebook.views.papers.PapersView
import org.denigma.kappa.notebook.views.simulations.SimulationsView
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

class TabsView(
               val elem: Element,
               val input: Var[KappaMessage],
               val out: Var[KappaMessage],
               val selector: Selector,
               val papers: Var[Map[String, Bookmark]],
               val kappaCursor: Var[Option[(Editor, PositionLike)]]
               /*
               val simulations: Var[Map[(Int, RunModel), SimulationStatus]],
               val launcher: Var[LaunchModel],

               val selector: Selector,
               val kappaCursor: Var[Option[(Editor, PositionLike)]]
               */
              ) extends BindableView {

  self =>

  val simulations: Var[Map[(Int, RunModel), SimulationStatus]] = Var(Map.empty[(Int, RunModel), SimulationStatus])

  val launcher: Var[LaunchModel] = Var( LaunchModel("", RunModel(code = "", max_events = Some(10000), max_time = None)))

  lazy val selected = selector.tab

  override def bindView() = {
    super.bindView()
    val launcher: Var[LaunchModel] = Var( LaunchModel("", RunModel(code = "", max_events = Some(10000), max_time = None)))
    import com.softwaremill.quicklens._
    val l: LaunchModel = launcher.now
    //val launchParams = l.modify(_.parameters).setTo(l.parameters.copy(code = concat()))
    //out() = launchParams
    input.foreach{
      case SimulationResult(server, status, token, params) =>
        println("percent: "+ status.percentage)
        simulations() = simulations.now.updated((token, params.getOrElse(status.runParameters)), status)
        //if(errors.now.nonEmpty) errors() = List.empty
      case other => //do nothing
    }
  }


  protected def defaultContent = ""
  protected def defaultLabel = ""

  type Item = Rx[common.TabItem]



  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  override lazy val injector = defaultInjector
    .register("Simulations") {
      case (el, params) =>
        new SimulationsView(el, selected, simulations, launcher).withBinder(new CodeBinder(_))
    }
    /*
    .register("Console") {
      case (el, params) =>
        new ConsoleView(el, hub.console, selected).withBinder(new CodeBinder(_))
    }
    */

    .register("Image") {
      case (el, params) =>
        new ImageView(el, selected, selector.image).withBinder(new CodeBinder(_))
    }

    .register("Papers") {
      case (el, params) =>
        new PapersView(el, selected, papers, selector, kappaCursor).withBinder(new CodeBinder(_))
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