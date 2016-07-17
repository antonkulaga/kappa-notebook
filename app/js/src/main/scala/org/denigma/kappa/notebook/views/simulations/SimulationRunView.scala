package org.denigma.kappa.notebook.views.simulations


import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsMapView, UpdatableView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, SimulationResult}
import org.denigma.kappa.messages.WebSimMessages.{KappaPlot, RunModel, SimulationStatus}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.ServerConnections
import org.denigma.kappa.notebook.views.common._
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.{Element, Event}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.Predef.Map
import scala.collection.immutable._
import scala.util._

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
