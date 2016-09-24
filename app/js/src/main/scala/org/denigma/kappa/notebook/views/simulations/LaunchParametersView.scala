package org.denigma.kappa.notebook.views.simulations

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.kappa.messages.KappaSourceFile
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.WebSimMessages.SimulationStatus
import org.denigma.kappa.notebook.views.common.SimpleFileView
import org.scalajs.dom.raw.{Element, MouseEvent}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable.Seq

class LaunchParametersView(val elem: Element,
                           val simulation: Rx[SimulationStatus],
                           val code: Rx[String],
                           val params: Option[LaunchModel],
                           val selected: Rx[String]
                       ) extends CollectionSeqView
{
  self=>

  type Item = KappaSourceFile

  override type ItemView = SimpleFileView

  val active: Rx[Boolean] = selected.map(s=>s=="parameters")

  val events: Rx[Int] = simulation.map(sim=>sim.event.getOrElse(0))

  val points: Rx[Int] = simulation.map(sim=>sim.nb_plot.getOrElse(0))

  var time: Rx[Double] = simulation.map(sim=>sim.time)

  val console: Rx[String] = simulation.map(sim=>sim.log_messages.foldLeft(""){
    case (acc, e) => acc + "\n" + e
  })

  val implicitSignature = Var(true)

  protected val maxTime = simulation.map(sim=>sim.max_time)

  protected val maxEvents =simulation.map(sim=>sim.max_events)

  val saveConcat: Var[MouseEvent] = Var(Events.createMouseEvent())

  saveConcat.triggerLater{
    saveAs(selected.now+".ka", console.now)
  }


  override val items: Rx[List[KappaSourceFile]] = Var(params.map(l=>l.files.map{ case (path, content) =>KappaSourceFile(path, content, saved = true)}).getOrElse(Nil))

  override def newItemView(item: KappaSourceFile): ItemView = this.constructItemView(item) {
    case (el, _) => new SimpleFileView(el, Var(item)).withBinder(v=>new GeneralBinder(v))

  }

}