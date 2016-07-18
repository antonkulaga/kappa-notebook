package org.denigma.kappa.notebook.views.simulations


import org.denigma.binding.views.{BindableView, UpdatableView}
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.WebSimMessages.{KappaPlot, SimulationStatus}
import org.denigma.kappa.notebook.views.common._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

class SimulationRunView(val elem: Element,
                        token: Int,
                        params: Option[LaunchModel],
                        val selected: Var[String],
                        val simulation: Var[SimulationStatus] = Var(SimulationStatus.empty))
  extends BindableView with UpdatableView[SimulationStatus] with TabItem
{

  val tab: Var[String] = Var("plot")

  val plot: Rx[KappaPlot] = simulation.map(s=>s.plot.getOrElse(KappaPlot.empty))

  val fluxMap = simulation.map(s=>s.flux_maps)


  //val selected = simulation.map(s=>s.st)
  override def update(value: SimulationStatus) = {
    simulation() = value
    this
  }


  override lazy val injector = defaultInjector
    .register("Plot") {
      case (el, _) =>
        new ChartView(el, Var(id), plot, tab).withBinder(new FixedBinder(_))
    }
    .register("Parameters") {
      case (el, _) =>
        new LaunchParametersView(el, simulation, tab).withBinder(new FixedBinder(_))
    }
/*
    .register("FluxMaps") {
      case (el, params) =>
        new Flux(el, Var(id), plot).withBinder(new FixedBinder(_))
    }
  */
}
