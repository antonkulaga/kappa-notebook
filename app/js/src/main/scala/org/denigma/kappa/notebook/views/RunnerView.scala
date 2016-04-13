package org.denigma.kappa.notebook.views

import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.controls.code.CodeBinder
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{Element, HTMLInputElement, HTMLTextAreaElement}
import rx._
import org.denigma.binding.binders.{Events, ReactiveBinder}
import org.denigma.binding.macroses._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.kappa.notebook.KappaHub

class RunnerView(val elem: Element, val name: Var[String], hub: KappaHub) extends BindableView
{
  self=>

  val parameters = Var(hub.runParameters.now)

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
    name() = fn
    val popt = if(p<=0) None else Some(p)
    //println(s"params = g($g) and s($s)")
    //val newParams = parameters.now.copy( fn, optInt(ev), opt(t), p )
    val newParams = parameters.now.copy(max_events = optInt(ev), max_time = opt(t), nb_plot = popt)
    parameters.set(newParams)
  }


  val run = Var(org.denigma.binding.binders.Events.createMouseEvent)
  run.triggerLater{
    //dom.console.log("sending the code...")
    hub.runParameters.Internal.value = parameters.now.copy(code = hub.kappaCode.now.text)
    hub.runParameters.propagate()
  }

  name.onChange{  case n=>  if(fileName.now!=n) fileName() = n  }

}