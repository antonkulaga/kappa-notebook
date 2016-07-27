package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.views.{ItemsMapView, ItemsSeqView}
import org.denigma.kappa.messages.WebSimMessages.FluxMap
import org.denigma.kappa.notebook.views.common.{FixedBinder, TabHeaders}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable.SortedSet


class FluxesView(val elem: Element, val items: Rx[Map[String, FluxMap]], tab: Rx[String]) extends ItemsMapView{

  val active: Rx[Boolean] = tab.map(s=>s=="fluxes")

  val notEmpty: Rx[Boolean] = items.map(its=>its.nonEmpty)

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