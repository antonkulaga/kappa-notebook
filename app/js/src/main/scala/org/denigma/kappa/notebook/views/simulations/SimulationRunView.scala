package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.binders.Events
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.{KappaMessage, SimulationCommands}
import org.denigma.kappa.messages.ServerMessages.{LaunchModel}
import org.denigma.kappa.messages.WebSimMessages.{FluxMap, KappaPlot, SimulationStatus}
import org.denigma.kappa.model.KappaModel.KappaSnapshot
import org.denigma.kappa.notebook.views.common._
import org.denigma.kappa.notebook.views.simulations.fluxes.FluxesView
import org.denigma.kappa.notebook.views.simulations.snapshots.SnapshotsView
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import org.denigma.binding.extensions._
import org.denigma.kappa.messages.KappaMessage.ServerCommand
import rx._

/**
  * The view for each Simulation Run (contains subviews with plots, fluxes, console, snapshots, etc)
  * @param elem
  * @param token
  * @param params
  * @param simulation
  * @param input
  * @param selected
  */
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

  val initialCode = simulation.map(sim=>/*sim.code.orElse(params.map(_.fullCode)*/ sim.code)//.getOrElse("### NODE CODE AVALIABLE ###"))

  val plot: Rx[KappaPlot] = simulation.map{s=>
    s.plot.getOrElse(KappaPlot.empty)
  }

  val currentEvent = simulation.map(s=>s.event)

  val logMessages =  simulation.map(s=>s.log_messages)

  val deadlocks: Rx[List[String]] = logMessages.map{ messages => messages.filter{ m => m.contains("deadlock was reached")}}

  val percentage: Rx[Double] = simulation.map(s=>s.percentage)

  val percentageString = percentage.map(p=>s"$p%")

  val isRunning = simulation.map{ s => s.is_running}

  val stopClick = Var(Events.createMouseEvent())
  stopClick.onChange{ ev =>
    input() = ServerCommand(ServerCommand.defaultServer, SimulationCommands.StopSimulation(token)) //TODO: fix this terrible code
  }


  //val succeeded = percentage.map(p => p >= 100)

  //val stopped = simulation.map{ s => s.stillRunning }

  val statusString = simulation.map{ s =>
    if(s.percentage >= 100.0) "succeeded" else
    if(s.stopped) "stopped"
    else "running"
  }

  val hasDeadlock: Rx[Boolean] = deadlocks.map(d=>d.nonEmpty)

  val deadlockString: Rx[String] = deadlocks.map{ d => d.foldLeft(""){ case (acc, e) => acc + e +"\n"}}

  lazy val fluxMap: Rx[Map[String, FluxMap]] = simulation.map(s=>s.flux_maps.map(fl=>fl.flux_name ->fl).toMap)

  val hasFluxes = fluxMap.map(fl=>fl.nonEmpty)

  val snapshots: Rx[List[KappaSnapshot]] = simulation.map{ s =>
    val kappaSnapshots = s.snapshots.map(snap=>snap.toKappaSnapshot)
    kappaSnapshots
  }

  val hasSnapshots = snapshots.map(snp=>snp.nonEmpty)

  override lazy val injector = defaultInjector
    .register("Plot") {
      case (el, _) =>
        new SimulationPlotView(el, Var(id), plot, tab).withBinder(new CodeBinder(_))
    }
    .register("Parameters") {
      case (el, _) =>
        new LaunchParametersView(el, simulation, initialCode, params, tab).withBinder(new CodeBinder(_))
    }
    .register("Console") {
      case (el, _) =>
        new ConsoleView(el, logMessages, tab).withBinder(new CodeBinder(_))
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










