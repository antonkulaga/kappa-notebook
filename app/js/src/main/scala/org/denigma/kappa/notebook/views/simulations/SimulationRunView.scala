package org.denigma.kappa.notebook.views.simulations


import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, UpdatableView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.WebSimMessages.{FluxMap, KappaPlot, SimulationStatus}
import org.denigma.kappa.notebook.views.common._
import org.denigma.kappa.notebook.views.simulations.fluxes.FluxesView
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

class SimulationRunView(val elem: Element,
                        val token: Int,
                        val params: Option[LaunchModel],
                        val selected: Var[String],
                        val simulation: Var[SimulationStatus] = Var(SimulationStatus.empty))
  extends BindableView with TabItem
{

  val tab: Var[String] = Var("plot")//Var("fluxes")

  lazy val plot: Rx[KappaPlot] = simulation.map(s=>s.plot.getOrElse(KappaPlot.empty))

  val fluxMap: Dynamic[Map[String, FluxMap]] = simulation.map(s=>s.flux_maps.map(fl=>fl.flux_name ->fl).toMap)
  override lazy val injector = defaultInjector
    .register("Plot") {
      case (el, _) =>
        new ChartView(el, Var(id), plot, tab).withBinder(new CodeBinder(_))
    }
    .register("Parameters") {
      case (el, _) =>
        new LaunchParametersView(el, simulation, params, tab).withBinder(new CodeBinder(_))
    }
    .register("Console") {
      case (el, _) =>
        new ConsoleView(el, simulation.map(s=>s.log_messages), tab).withBinder(new CodeBinder(_))
    }
    .register("Fluxes") {
      case (el, _) =>
        new FluxesView(el, fluxMap, tab).withBinder(new CodeBinder(_))
    }
}
