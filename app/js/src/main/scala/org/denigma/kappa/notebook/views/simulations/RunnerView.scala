package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.kappa.messages.LaunchModel
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

class RunnerView(val elem: Element, launcher: Var[LaunchModel]) extends BindableView
{
  self=>

  val parameters = Var(launcher.now.parameters)

  def optInt(n: Int): Option[Int] = if(n > 0.0) Some(n) else None

  def opt(n: Double): Option[Double] = if(n > 0.0) Some(n) else None

  val events: Var[Int] = Var(10000)

  var time: Var[Double] = Var(0.0)

  val points: Var[Int] = Var(250)

  val fileName = Var("model.ka")

  val implicitSignature = Var(true)

  val gluttony: Var[Boolean] = Var(false)

  val output: Rx[Unit] = Rx{
    val fn: String = fileName()
    val ev: Int = self.events()
    val t: Double = self.time()
    val p: Int = self.points()
    val s: Boolean = self.implicitSignature()
    val g = self.gluttony()
    //name() = fn
    val popt = if(p<=0) None else Some(p)
    //println(s"params = g($g) and s($s)")
    //val newParams = parameters.now.copy( fn, optInt(ev), opt(t), p )
    val newParams = parameters.now.copy(max_events = optInt(ev), max_time = opt(t), nb_plot = popt)
    parameters.set(newParams)
  }


  val run = Var(org.denigma.binding.binders.Events.createMouseEvent)
  run.triggerLater{
    //dom.console.log("sending the code...")
    //hub.runParameters.Internal.value = parameters.now.copy(code = hub.kappaCode.now.text)
    //hub.runParameters.propagate()
    val params = launcher.now
    launcher() = params.copy(parameters = this.parameters.now, counter = params.counter + 1)
  }

  //name.onChange{  case n=>  if(fileName.now!=n) fileName() = n  }

}