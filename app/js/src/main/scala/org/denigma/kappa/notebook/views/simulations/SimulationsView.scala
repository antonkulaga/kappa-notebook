package org.denigma.kappa.notebook.views.simulations


import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.commons.Uploader
import org.denigma.binding.views.{BindableView, CollectionMapView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, SimulationResult, SyntaxErrors}
import org.denigma.kappa.messages.WebSimMessages.SimulationStatus
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.common._
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable._

class SimulationsView(val elem: Element,
                      val sourceMap: Rx[Map[String, KappaSourceFile]],
                      val input: Rx[KappaMessage],
                      val output: Var[KappaMessage],
                      val serverConnections: Var[ServerConnections]
                     )
  extends BindableView with Uploader /*with TabItem*/ with CollectionMapView //with CollectionSortedSetView
{
  self=>

  lazy val headers = itemViews.map(its=>SortedSet.empty[String] ++ its.values.map(_.id))

  val tab = Var("runner")

  val runnerActive: Rx[Boolean] = tab.map(tb=>tb=="runner")

  override type Key = (Int, Option[LaunchModel])

  override type Value = SimulationStatus

  override type ItemView = SimulationRunView

  val items: Var[Map[Key, Value]] = Var(Map.empty[Key, Value])

  lazy val errors = Var(List.empty[String])

  input.foreach{
    case KappaMessage.ServerResponse(server, SimulationResult(status, token, params) ) =>
      errors() = List.empty[String]
      items() = items.now.updated((token, params), status)
      require(items.now.exists{ case (key, value) => value==status}, "statu should be added to items")

    case ServerErrors(list) =>
      dom.console.error("server errors = "+list)
      errors() = list

    case KappaMessage.ServerResponse(server, s: SyntaxErrors) => errors() = s.errors.map(e=>e.fullMessage)


    //if(errors.now.nonEmpty) errors() = List.empty
    case other => //do nothing
  }


  def makeId(item: Key): String = "#"+item._1

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
    .register("ServerErrors")((el, args) => new ServerErrorsView(el, errors).withBinder(n => new CodeBinder(n)))

  override def updateView(view: SimulationRunView, key: (Int, Option[LaunchModel]), old: SimulationStatus, current: SimulationStatus): Unit = {
    view.simulation() = current
  }
}