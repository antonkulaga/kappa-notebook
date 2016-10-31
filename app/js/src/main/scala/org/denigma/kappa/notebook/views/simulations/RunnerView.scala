package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.SourcesFileSelector
import org.denigma.kappa.notebook.circuits.SimulationsCircuit
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

/**
  * Runner view (displayed on Create button)
  * @param elem element to bind to
  * @param tab tab name
  * @param circuit circuit that handles simulations-related events
  */
class RunnerView(val elem: Element,
                 val tab: Var[String],
                 val circuit: SimulationsCircuit
                 ) extends BindableView//FixedCollectionSeqView
{
  self=>

  val active: Dynamic[Boolean] = tab.map(v=>v=="runner")

  protected def optInt(n: Int): Option[Int] = if(n > 0.0) Some(n) else None

  protected def opt(n: Double): Option[Double] = if(n > 0.0) Some(n) else None

  val events: Var[Int] = Var(10000)

  var time: Var[Double] = Var(0.0)

  //val points: Var[Int] = Var(250)

  val implicitSignature = Var(true)

  protected val maxTime = time.map(t => if(t > 0) Some(t) else None)
  protected val maxEvents = events.map(ev=> if(ev> 0) Some(ev) else None)
  //protected val plotPeriod = points.map(p=> if(p>0) Some(p) else None)
  val plotPeriod = Var(0.1)

  protected def launch() = {
    val params = LaunchModel(Nil, plot_period = self.plotPeriod.now, max_events = self.maxEvents.now, max_time = self.maxTime.now)
    circuit.launch(params)
  }

  val run = Var(org.denigma.binding.binders.Events.createMouseEvent)
  run.triggerLater{ launch() }
}