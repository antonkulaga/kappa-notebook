package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.extensions.sq
import org.denigma.binding.views.{BindableView, ItemsMapView, ItemsSeqView}
import org.denigma.kappa.messages.WebSimMessages.FluxMap
import org.denigma.kappa.notebook.views.common.{TabHeaders, FixedBinder}
import org.denigma.kappa.notebook.views.visual.rules.Visualizer
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import org.denigma.binding.extensions._
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.collection.immutable.SortedSet


class FluxesView(val elem: Element, val items: Rx[Map[String, FluxMap]], tab: Rx[String]) extends ItemsMapView{

  val active: Rx[Boolean] = tab.map(s=>s=="fluxes")

  val selected: Var[String] = Var("")

  val headers = itemViews.map(its=>SortedSet.empty[String] ++ its.values.map(_.id))

  type Item = String

  type Value = FluxMap

  type ItemView = FluxView

  override def newItemView(item: Item): FluxView = constructItemView(item){
    case (el, mp) =>
      selected() = item
      new FluxView(el, item, Var(items.now(item)), selected).withBinder(v=> new FixedBinder(v))
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected).withBinder(new FixedBinder(_)))

}
