package org.denigma.kappa.notebook.views.simulations


import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionMapView, CollectionSortedSetView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.WebSimMessages.SimulationStatus
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.actions.Commands
import org.denigma.kappa.notebook.circuits.SimulationsCircuit
import org.denigma.kappa.notebook.views.common._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._

class SimulationsView(val elem: Element,
                      val circuit: SimulationsCircuit
                     )
  extends BindableView with Uploader /*with TabItem*/ with CollectionMapView //with CollectionSortedSetView
{
  self=>

  override lazy val items: Rx[Map[Key, Value]] = circuit.simulationResults

  lazy val headers = itemViews.map(its=>SortedSet.empty[String] ++ its.values.map(_.id))

  lazy val tab = Var("runner")

  lazy val runnerActive: Rx[Boolean] = tab.map(tb=>tb=="runner")

  override type Key = (Int, Option[LaunchModel])

  override type Value = SimulationStatus

  override type ItemView = SimulationRunView


  def makeId(item: Key): String = "#"+item._1

  override def newItemView(key: Key, value: Value): SimulationRunView = this.constructItemView(key)( {
    case (el, mp) =>
      el.id =  makeId(key) //bad practice
      val view = new SimulationRunView(el, key._1, key._2, tab, Var(value)).withBinder(new CodeBinder(_))
      tab() = view.id
      view
  })


  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, tab)(str=>str).withBinder(new GeneralBinder(_)))
    .register("runner")((el, args) => new RunnerView(el, tab, circuit.configurations, circuit.launcher).withBinder(n => new CodeBinder(n)))
    .register("ServerErrors")((el, args) => new ServerErrorsView(el, circuit.serverErrors.map(e=>e.errors)).withBinder(n => new CodeBinder(n)))

  override def updateView(view: SimulationRunView, key: (Int, Option[LaunchModel]), old: SimulationStatus, current: SimulationStatus): Unit = {
    println("status updated "+current.plot.map(p=>p.legend))
    view.simulation() = current
  }
}

class SimulationsHeaders(val elem: Element, val items: Rx[SortedSet[String]], val input: Var[KappaMessage], val selected: Var[String])
                        (implicit getCaption: String => String) extends CollectionSortedSetView {

  override type Item =  String

  override type ItemView = SimulationTabItemView

  override def newItemView(item: Item): ItemView= constructItemView(item){
    case (el, _) => new SimulationTabItemView(el, item, input,  selected)(getCaption).withBinder(new GeneralBinder(_))
  }
}

class SimulationTabItemView(val elem: Element,
                            itemId: String,
                            val input: Var[KappaMessage], val selected: Var[String] )(implicit getCaption: String => String) extends BindableView {

  val caption: Var[String] = Var(getCaption(itemId))

  val active: rx.Rx[Boolean] = selected.map(value => value == itemId)

  val select = Var(Events.createMouseEvent())
  select.triggerLater({
    selected() = itemId
  })

  val closeClick = Var(Events.createMouseEvent())
  closeClick.onChange{
    ev=> input() = Commands.CloseFile(itemId)
  }
}
