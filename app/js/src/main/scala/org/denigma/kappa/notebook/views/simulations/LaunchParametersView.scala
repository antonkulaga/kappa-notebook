package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.kappa.messages.{KappaFile, KappaMessage}
import org.denigma.kappa.messages.KappaMessage.ServerCommand
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.WebSimMessages.{RunModel, SimulationStatus}
import org.scalajs.dom.raw.{Element, MouseEvent}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import com.softwaremill.quicklens._
import org.denigma.binding.binders.Events
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.views.common.TabItem
import org.scalajs.dom
import org.scalajs.dom.Event
import rx.Rx.Dynamic

class LaunchParametersView(val elem: Element,
                           val simulation: Rx[SimulationStatus],
                           params: Option[LaunchModel],
                           val selected: Rx[String]
                       ) extends BindableView//FixedCollectionSeqView
{
  self=>

  val active: Rx[Boolean] = selected.map(s=>s=="parameters")

  def optInt(n: Int): Option[Int] = if(n > 0.0) Some(n) else None

  def opt(n: Double): Option[Double] = if(n > 0.0) Some(n) else None

  val events: Rx[Int] = simulation.map(sim=>sim.event.getOrElse(0))

  val points: Rx[Int] = simulation.map(sim=>sim.nb_plot.getOrElse(0))

  var time: Rx[Double] = simulation.map(sim=>sim.time)

  val console: Rx[String] = simulation.map(sim=>sim.log_messages.foldLeft(""){
    case (acc, e) => acc + "\n" + e
  })

  val code = simulation.map(sim=>sim.code.orElse(params.map(_.fullCode)).getOrElse("### NODE CODE AVALIABLE ###"))
  dom.console.error(simulation.now.code.toString)
  //val points =  = simulation.map(sim=>sim.)

  val implicitSignature = Var(true)

  // val gluttony: Var[Boolean] = Var(false)

  protected val maxTime = simulation.map(sim=>sim.max_time)
  protected val maxEvents =simulation.map(sim=>sim.max_events)


  val saveOutput: Var[MouseEvent] = Var(Events.createMouseEvent())

  saveOutput.triggerLater{
    saveAs("log.txt", console.now)
  }
  //protected val nbPlot = points.map(p=> if(p>0) Some(p) else None)

}