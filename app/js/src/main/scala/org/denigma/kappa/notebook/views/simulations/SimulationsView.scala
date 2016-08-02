package org.denigma.kappa.notebook.views.simulations


import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.commons.Uploader
import org.denigma.binding.views.{BindableView, CollectionMapView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, SimulationResult}
import org.denigma.kappa.messages.WebSimMessages.SimulationStatus
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.common._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._

class SimulationsView(val elem: Element,
                      val sourceMap: Rx[Map[String, KappaFile]],
                      val input: Rx[KappaMessage],
                      val output: Var[KappaMessage],
                      val serverConnections: Var[ServerConnections]
                     )
  extends BindableView with Uploader /*with TabItem*/ with CollectionMapView //with CollectionSortedSetView
{
  self=>

  val headers = itemViews.map(its=>SortedSet.empty[String] ++ its.values.map(_.id))
  //this.items.map{ case its=> SortedSet.empty[String] ++ its.keySet.map(makeId) }

  val tab = Var("runner")

  override type Key = (Int, Option[LaunchModel])

  override type Value = SimulationStatus

  override type ItemView = SimulationRunView

  val items: Var[Map[Key, Value]] = Var(Map.empty[Key, Value])

  input.foreach{
    case KappaMessage.ServerResponse(server, SimulationResult(status, token, params) ) =>
      //println("percent: "+ status.percentage)
      items() = items.now.updated((token, params), status)

    //if(errors.now.nonEmpty) errors() = List.empty
    case other => //do nothing
  }


  def makeId(item: Item): String = "#"+item._1

  override def newItemView(key: Key, value: Value): SimulationRunView = this.constructItemView(key)( {
    case (el, mp) =>
      el.id =  makeId(key) //bad practice
      val view = new SimulationRunView(el, key._1, key._2, tab, Var(SimulationStatus.empty)).withBinder(new CodeBinder(_))
      tab() = view.id
      view
  })


  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, tab)(str=>str).withBinder(new GeneralBinder(_)))
    .register("runner")((el, args) => new RunnerView(el, tab, output, serverConnections, sourceMap).withBinder(n => new CodeBinder(n)))

  override def updateView(view: SimulationRunView, key: (Int, Option[LaunchModel]), old: SimulationStatus, current: SimulationStatus): Unit = {
    view.simulation() = current
  }
}
