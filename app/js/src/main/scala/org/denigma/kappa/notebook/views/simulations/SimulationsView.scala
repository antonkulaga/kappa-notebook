package org.denigma.kappa.notebook.views.simulations


import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.commons.Uploader
import org.denigma.binding.views.{BindableView, ItemsMapView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, SimulationResult}
import org.denigma.kappa.messages.WebSimMessages.SimulationStatus
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.ServerConnections
import org.denigma.kappa.notebook.views.common._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.Predef.Map
import scala.collection.immutable._

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

  override def newItemView(item: Key): SimulationRunView = this.constructItemView(item)( {
    case (el, mp) =>
      el.id =  makeId(item) //bad practice
      val view = new SimulationRunView(el, item._1, item._2, selectTab, Var(SimulationStatus.empty)).withBinder(new CodeBinder(_))
      selectTab() = view.id
      view
  })


  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selectTab).withBinder(new GeneralBinder(_)))
    .register("runner")((el, args) => new RunnerView(el, output, serverConnections, sourceMap).withBinder(n => new CodeBinder(n)))
}
