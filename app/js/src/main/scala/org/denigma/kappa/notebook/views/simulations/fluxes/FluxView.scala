package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.views.{BindableView, ItemsSeqView, ItemsSetView, UpdatableView}
import org.denigma.kappa.messages.WebSimMessages.FluxMap
import org.scalajs.dom.raw.{Element, SVGElement}
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.graph.{KappaEdgeVisualSettings, KappaNodeVisualSettings, LineParams}
import org.denigma.kappa.notebook.views.common.FixedBinder
import org.scalajs.dom
import org.scalajs.dom.svg.SVG
import rx.Rx.Dynamic

import scala.collection.immutable.{Iterable, SortedSet}

class FluxView(val elem: Element, val name: String, val item: Var[FluxMap], val tab: Rx[String]) extends ItemsSetView
  with UpdatableView[FluxMap]
{

  type Item = RuleFlux

  override type ItemView = HitsView

  lazy val container = elem.selectByClass("graph")

  val fluxName: Rx[String] =  item.map(fl=>fl.flux_name)

  val start = item.map(fl=>fl.flux_begin_time)

  val end = item.map(fl=>fl.flux_begin_time)
  val other = item.map(fl => fl.flux_rules)
  val hits = item.map(fl => fl.flux_hits)
  val fluxes = item.map(fl => fl.flux_fluxs)


  val active = tab.map(t=>t==name)

  override def update(value: FluxMap)= {
    item()  = value
    this
  }

  override def items: Rx[SortedSet[RuleFlux]] = item.map(i => RuleFlux.fromFluxMap(i))

  override def newItemView(item: Item): ItemView = this.constructItemView(item){
    case (el, _) => new HitsView(el, item).withBinder(v => new FixedBinder(v))
  }

  val s =dom.document.getElementById("canvas") match {
    case s: SVG => s
    case other => throw new Exception("cannot find SVG canvas from FluxView")
  }

  override lazy val injector = defaultInjector
    .register("FluxGraphView") { case (el, args) =>
      new FluxGraphView(el, items, new KappaNodeVisualSettings(16, 6), new KappaEdgeVisualSettings(14, 4, LineParams()), s).withBinder(v => new FixedBinder(v))
    }
}