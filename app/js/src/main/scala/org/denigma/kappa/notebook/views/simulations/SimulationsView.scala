package org.denigma.kappa.notebook.views.simulations


import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionMapView, CollectionSeqView, CollectionSortedSetView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage.ServerCommand
import org.denigma.kappa.messages.ServerMessages.LaunchModel
import org.denigma.kappa.messages.WebSimMessages.SimulationStatus
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.actions.Commands
import org.denigma.kappa.notebook.circuits.{ErrorsCircuit, SimulationsCircuit}
import org.denigma.kappa.notebook.views.common._
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._

/**
  * View for all simulations
  * @param elem HTML element to bind to
  * @param simulationCircuit circuit that handles simulation events
  * @param errorsCircuit circuit that handles error-related events
  */
class SimulationsView(val elem: Element,
                      val simulationCircuit: SimulationsCircuit,
                      val errorsCircuit: ErrorsCircuit
                     )
  extends BindableView with Uploader /*with TabItem*/ with CollectionMapView //with CollectionSortedSetView
{
  self=>


  lazy val headers: Var[List[(Int, Option[LaunchModel])]] = simulationCircuit.openOrder

  override lazy val items: Rx[Map[Key, Value]] = simulationCircuit.simulationResults

  lazy val tab = Var("runner")

  lazy val runnerActive: Rx[Boolean] = tab.map(tb=>tb=="runner")

  override type Key = (Int, Option[LaunchModel])

  override type Value = SimulationStatus

  override type ItemView = SimulationRunView

  def makeId(item: Key): String = "#"+item._1

  override def newItemView(key: Key, value: Value): SimulationRunView = this.constructItemView(key)( {
    case (el, mp) =>
      val (token, initial) = key
      el.id =  makeId(key) //bad practice
      val view = new SimulationRunView(el, token, initial, Var(value), simulationCircuit.input, tab).withBinder(new CodeBinder(_))
      tab() = makeId(key)
      view
  })

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new SimulationsHeaders(el, headers, simulationCircuit.input, tab)((token, initial)=> "#"+token).withBinder(new GeneralBinder(_)))
    .register("runner")((el, args) => new RunnerView(el, tab, simulationCircuit).withBinder(n => new CodeBinder(n)))
    .register("ServerErrors")((el, args) => new ServerErrorsView(el, errorsCircuit.serverErrors.map(e=>e.errors)).withBinder(n => new CodeBinder(n)))

  override def updateView(view: SimulationRunView, key: (Int, Option[LaunchModel]), old: SimulationStatus, current: SimulationStatus): Unit = {
    view.simulation() = current
  }
}

class SimulationsHeaders(val elem: Element, val items: Rx[List[(Int, Option[LaunchModel])]], val input: Var[KappaMessage], val selected: Var[String])
                        (implicit getCaption: (Int, Option[LaunchModel]) => String) extends CollectionSeqView {

  override type Item = (Int, Option[LaunchModel])

  override type ItemView = SimulationTabItemView

  override def newItemView(item: Item): ItemView= constructItemView(item){
    case (el, _) =>
      val (token, initial) = item
      el.id = "#" + token.toString
      new SimulationTabItemView(el, token, initial, input,  selected)(getCaption).withBinder(new GeneralBinder(_))
  }
}

/**
  * View for a tab with simulation
  * @param elem
  * @param token
  * @param initial
  * @param input
  * @param selected
  * @param getCaption
  */
class SimulationTabItemView(val elem: Element,
                            token: Int, initial: Option[LaunchModel],
                            val input: Var[KappaMessage], val selected: Var[String] )(implicit getCaption: (Int, Option[LaunchModel]) => String) extends BindableView {

  val caption: Var[String] = Var(getCaption(token, initial))

  val active: rx.Rx[Boolean] = selected.map(value => value == "#"+token.toString)

  val select = Var(Events.createMouseEvent())
  select.triggerLater({
    selected() = "#"+token.toString
  })

  val closeClick = Var(Events.createMouseEvent())
  closeClick.onChange{
    ev=>
      dom.console.log(s"close $token")
      input() = ServerCommand.onDefaultServer(SimulationCommands.CloseSimulation(token, initial))
  }
}
