package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.SourcesFileSelector
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

class RunnerView(val elem: Element,
                 val tab: Var[String],
                 val configurations: Rx[Map[String, SourcesFileSelector]],
                 val runner: Var[(LaunchModel, String)]
                 ) extends BindableView//FixedCollectionSeqView
{
  self=>

  val active: Dynamic[Boolean] = tab.map(v=>v=="runner")

  tab.onChange{ value => println("TAB CHANGED TO "+value) }

  protected def optInt(n: Int): Option[Int] = if(n > 0.0) Some(n) else None

  protected def opt(n: Double): Option[Double] = if(n > 0.0) Some(n) else None

  val events: Var[Int] = Var(100000)

  var time: Var[Double] = Var(0.0)

  val points: Var[Int] = Var(250)

  val implicitSignature = Var(true)


  // val gluttony: Var[Boolean] = Var(false)

  protected val maxTime = time.map(t => if(t > 0) Some(t) else None)
  protected val maxEvents = events.map(ev=> if(ev> 0) Some(ev) else None)
  protected val nbPlot = points.map(p=> if(p>0) Some(p) else None)

  protected def launch() = {
    val params = LaunchModel(Nil, nb_plot = self.nbPlot.now, max_events = self.maxEvents.now, max_time = self.maxTime.now)
    runner() = (params, "")
  }

  val run = Var(org.denigma.binding.binders.Events.createMouseEvent)
  run.triggerLater{ launch() }
}