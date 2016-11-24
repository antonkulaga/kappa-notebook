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
import org.denigma.binding.extensions._


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

  val implicitSignature = Var(true)

  protected val maxTime: Rx[Option[Double]] = time.map(t => if(t > 0) Some(t) else None)
  protected val maxEvents: Rx[Option[Int]] = events.map(ev=> if(ev> 0) Some(ev) else None)

  //protected val plotPeriod = points.map(p=> if(p>0) Some(p) else None)

  val plotPeriod = Var(1.0)

  val timeEvents = Rx{(maxTime(), maxEvents())}
  protected val divider = 100.0 //period divider

  protected def rp(value: Double): Int = Math.round(value) match {
    case 0.0 => 1
    case other => other.toInt
  }

  timeEvents.onChange{
    case (Some(t), Some(e)) => if(plotPeriod.now > t) plotPeriod() = t / divider
    case (Some(t), None) => if(plotPeriod.now > t) plotPeriod() = t / divider
    case (None, Some(e)) =>
      val pl = plotPeriod.now
      val r =rp(e / divider)
      if(pl> e) plotPeriod() = Math.round( (e / divider) ) else if(pl!=rp(pl)) plotPeriod() = Math.round(pl)
    case (None, None) =>
  }


  val runCount = Var(0)

  protected def launch(): Unit = {
    val params = LaunchModel(Nil, plot_period = self.plotPeriod.now, max_events = self.maxEvents.now, max_time = self.maxTime.now, runCount = runCount.now)
    if(circuit.run.now == params){
      runCount() = runCount.now + 1
      launch()
    } else circuit.launch(params)
  }

  val run = Var(org.denigma.binding.binders.Events.createMouseEvent)
  run.triggerLater{ launch() }
}