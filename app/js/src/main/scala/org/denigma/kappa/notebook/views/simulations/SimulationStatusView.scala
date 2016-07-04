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


class SimulationsView(val elem: Element,
                      val sourceMap: Rx[Map[String, KappaFile]],
                      val input: Rx[KappaMessage],
                      val output: Var[KappaMessage],
                      val serverConnections: Var[ServerConnections]
                     )

  extends BindableView with Uploader /*with TabItem*/ with ItemsMapView //with ItemsSetView
{
  self=>

  val headers = itemViews.map(its=>SortedSet.empty[String] ++ its.values.map(_.id))
  //this.items.map{ case its=> SortedSet.empty[String] ++ its.keySet.map(makeId) }

  val selectTab = Var("")

  override type Item = (Int, Option[LaunchModel])

  override type Value = SimulationStatus

  override type ItemView = SimulationStatusView

  val chartActive = Var(true)

  val outputActive = Var(true)

  val consoleOutput = Var("")

  val items: Var[Map[Key, Value]] = Var(Map.empty[Key, Value])

  input.foreach{
    case KappaMessage.ServerResponse(server, SimulationResult(status, token, params) ) =>
      //println("percent: "+ status.percentage)
      items() = items.now.updated((token, params), status)
    //if(errors.now.nonEmpty) errors() = List.empty
    case other => //do nothing
  }

  val saveOutput: Var[MouseEvent] = Var(Events.createMouseEvent())

  saveOutput.triggerLater{
    saveAs("log.txt", consoleOutput.now)
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
      case Success((file, text))=> consoleOutput.set(text)
      case Failure(th) => dom.console.error(s"File upload failure: ${th.toString}")
    })

  def makeId(item: Item): String = "#"+item._1

  override def newItemView(item: Key): SimulationStatusView = this.constructItemView(item)( {
    case (el, mp) =>
      el.id =  makeId(item) //bad practice
      val view = new SimulationStatusView(el, item._1, item._2, selectTab, Var(SimulationStatus.empty)).withBinder(new CodeBinder(_))
      selectTab() = view.id
      view
  })


  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selectTab).withBinder(new GeneralBinder(_)))
    .register("runner")((el, args) => new RunnerView(el, output, serverConnections, sourceMap).withBinder(n => new CodeBinder(n)))

}

class SimulationStatusView(val elem: Element,
                           token: Int,
                           params: Option[LaunchModel],
                           val selected: Var[String],
                           val simulation: Var[SimulationStatus] = Var(SimulationStatus.empty))
  extends BindableView with UpdatableView[SimulationStatus] with TabItem
{

  val console: Rx[String] = simulation.map{
    case s =>"" // s.log_messages.foldLeft("")((acc, el) => acc + el)
  }

  val plot: Rx[KappaPlot] = simulation.map(s=>s.plot.getOrElse(KappaPlot.empty))


  //val selected = simulation.map(s=>s.st)
  override def update(value: SimulationStatus) = {
    simulation() = value
    this
  }


  override lazy val injector = defaultInjector
    .register("Chart") {
      case (el, params) =>
        new ChartView(el, Var(id), plot).withBinder(new GeneralBinder(_))
    }
}
