package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.views.{CollectionMapView, CollectionSeqView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.WebSimMessages.FluxMap
import org.denigma.kappa.notebook.views.common.TabHeaders
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.collection.immutable.SortedSet


class FluxesView(val elem: Element, val items: Rx[Map[String, FluxMap]], tab: Rx[String]) extends CollectionMapView{

  lazy val active: Rx[Boolean] = tab.map(s=>s=="fluxes")

  lazy val nonEmpty: Rx[Boolean] = items.map(its=>its.nonEmpty)

  lazy val selected: Var[String] = Var("")

  val headers = itemViews.map(its=>SortedSet.empty[String] ++ its.values.map(_.id))

  type Key= String

  type Value = FluxMap

  type ItemView = FluxView

  override def newItemView(item: Key, value: Value): FluxView = constructItemView(item){
    case (el, mp) =>
      el.id = item
      selected() = item
      new FluxView(el, item, Var(value), selected).withBinder(v=> new CodeBinder(v))
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected).withBinder(new CodeBinder(_)))

  override def updateView(view: FluxView, key: String, old: FluxMap, current: FluxMap): Unit = {
    view.item() = current
  }
}