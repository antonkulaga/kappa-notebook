package org.denigma.kappa.notebook.views.simulations


import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsMapView, ItemsSetView, UpdatableView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.views.RunnerView
import org.denigma.kappa.notebook.views.common._
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Element, Event, SVGElement}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.List
import scala.Predef.{Map, Set}
import scala.collection.immutable._
import scala.util._


class SimulationsView(val elem: Element, val selected: Var[String], hub: KappaHub)
  extends BindableView with Uploader with TabItem with ItemsMapView //with ItemsSetView
{
  self=>

  val headers = itemViews.map(its=>SortedSet.empty[String] ++ its.values.map(_.id))
  //this.items.map{ case its=> SortedSet.empty[String] ++ its.keySet.map(makeId) }

  val selectTab = Var("")

  override type Item = (Int, RunModel)

  override type Value = SimulationStatus

  override type ItemView = SimulationStatusView

  val chartActive = Var(true)

  val outputActive = Var(true)

  val output = Var("")

  val saveOutput: Var[MouseEvent] = Var(Events.createMouseEvent())
  saveOutput.triggerLater{
    saveAs(hub.name.now, output.now)
  }

  val activateChart: Var[MouseEvent] = Var(Events.createMouseEvent())
  activateChart.triggerLater{
    chartActive() = !chartActive.now
  }

  val activateOutput: Var[MouseEvent] = Var(Events.createMouseEvent())
  activateOutput.triggerLater{
    outputActive() = !outputActive.now
  }

  val onUpload: Var[Event] = Var(Events.createEvent())
  onUpload.onChange(ev =>
    this.uploadHandler(ev){
      case Success((file, text))=> output.set(text)
      case Failure(th) => dom.console.error(s"File upload failure: ${th.toString}")
    })


  def makeId(item: Item): String = "#"+item._1

  override def items: Rx[Map[Key, SimulationStatus]] = hub.simulations

  override def newItemView(item: Key): SimulationStatusView = this.constructItemView(item)( {
    case (el, mp) =>
      el.id =  makeId(item) //bad practice
      val view = new SimulationStatusView(el, item._1, item._2, selectTab, Var(None)).withBinder(new CodeBinder(_))
      selectTab() = view.id
      view
  })

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selectTab).withBinder(new GeneralBinder(_)))
    .register("runner")((el, args) => new RunnerView(el, hub.name, hub).withBinder(n => new CodeBinder(n)))

}

class SimulationStatusView(val elem: Element,
                           token: Int,
                           params: RunModel,
                           val selected: Var[String],
                           val simulation: Var[Option[SimulationStatus]] = Var(None))
  extends BindableView with UpdatableView[SimulationStatus] with TabItem
{

  val console: Rx[String] = simulation.map{
    case Some(s) => s.log_messages.fold("") {  case arr => arr.foldLeft("")((acc, el) => acc + el)  }
    case None => ""
  }

  val series: Rx[List[Var[KappaSeries]]] = simulation.map{
    case Some(s)=>
      s.plot.map(KappaChart.fromKappaPlot).getOrElse(KappaChart.empty).series.map(Var(_))
    case None => KappaChart.empty.series.map(Var(_))
  }

  //val selected = simulation.map(s=>s.st)
  override def update(value: SimulationStatus) = {
    simulation() = Some(value)
    this
  }


  //val title = hub.name.map(_.replace(".out",""))
  override lazy val injector = defaultInjector
    .register("Chart") {
      case (el, params) =>
        new ChartView(el, Var(id), series).withBinder(new GeneralBinder(_))
    }
}
