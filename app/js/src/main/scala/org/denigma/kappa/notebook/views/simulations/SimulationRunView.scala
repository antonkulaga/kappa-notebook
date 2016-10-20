package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.binders.Events
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.WebSimMessages.{FluxMap, KappaPlot, SimulationStatus}
import org.denigma.kappa.notebook.views.common._
import org.denigma.kappa.notebook.views.simulations.fluxes.FluxesView
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._
import org.denigma.binding.extensions._
import org.denigma.kappa.messages.{KappaMessage}
import org.denigma.kappa.model.KappaModel.KappaSnapshot
import org.denigma.kappa.notebook.views.simulations.snapshots.SnapshotsView

class SimulationRunView(val elem: Element,
                        val token: Int,
                        val params: Option[LaunchModel],
                        val simulation: Var[SimulationStatus],
                        val input: Var[KappaMessage],
                        val selected: Var[String]
                       )
  extends BindableView with TabItem
{

  val tab: Var[String] = Var("plot")

  val initialCode = simulation.map(sim=>sim.code.orElse(params.map(_.fullCode)).getOrElse("### NODE CODE AVALIABLE ###"))

  val plot: Rx[KappaPlot] = simulation.map{s=>
    s.plot.getOrElse(KappaPlot.empty)
  }

  //val percentage = simulation.map(s=>s.percentage)

  //val maxOpt = simulation.map(s=>s.max)

  lazy val fluxMap:Rx[Map[String, FluxMap]] = simulation.map(s=>s.flux_maps.map(fl=>fl.flux_name ->fl).toMap)

  val hasFluxes = fluxMap.map(fl=>fl.nonEmpty)

  val snapshots: Rx[List[KappaSnapshot]] = simulation.map{ s =>
    val kappaSnapshots = s.snapshots.map(snap=>snap.toKappaSnapshot)
    println("KAPPA SNAPSHOTS ARE:")
    println(kappaSnapshots)
    kappaSnapshots
  }

  val hasSnapshots = snapshots.map(snp=>snp.nonEmpty)

  override lazy val injector = defaultInjector
    .register("Plot") {
      case (el, _) =>
        new ChartView(el, Var(id), plot, tab).withBinder(new CodeBinder(_))
    }
    .register("Parameters") {
      case (el, _) =>
        new LaunchParametersView(el, simulation, initialCode, params, tab).withBinder(new CodeBinder(_))
    }
    .register("Console") {
      case (el, _) =>
        new ConsoleView(el, simulation.map(s=>s.log_messages), tab).withBinder(new CodeBinder(_))
    }
    .register("Fluxes") {
      case (el, _) =>
        new FluxesView(el, fluxMap, tab).withBinder(new CodeBinder(_))
    }
    .register("Unary distances") {
      case (el, _) =>
        new FluxesView(el, fluxMap, tab).withBinder(new CodeBinder(_))
    }
    .register("Snapshots") {
      case (el, _) =>
        new SnapshotsView(el, snapshots, input, tab).withBinder(new CodeBinder(_))
    }
}










