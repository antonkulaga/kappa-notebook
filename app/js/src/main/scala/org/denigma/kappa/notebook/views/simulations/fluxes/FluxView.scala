package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.extensions._
import org.denigma.binding.views.{CollectionSortedSetView, UpdatableView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.WebSimMessages.FluxMap
import org.denigma.kappa.notebook.graph.{Colors, KappaEdgeVisualSettings, KappaNodeVisualSettings, LineParams}
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable.SortedSet

class FluxView(val elem: Element, val name: String, val item: Var[FluxMap], val tab: Rx[String]) extends CollectionSortedSetView
{

  type Item = RuleFlux

  override type ItemView = HitsView

  override lazy val items: Rx[SortedSet[RuleFlux]] = item.map(i => RuleFlux.fromFluxMap(i))

  lazy val nonEmpty: Rx[Boolean] = items.map(its=>its.nonEmpty)

  lazy val container = elem.selectByClass("graph")

  val fluxName: Rx[String] =  item.map(fl=>fl.flux_name)
  val start = item.map(fl=>fl.flux_begin_time)
  val end = item.map(fl=>fl.flux_begin_time)
  val other = item.map(fl => fl.flux_rules)
  val hits = item.map(fl => fl.flux_hits)
  val fluxes = item.map(fl => fl.flux_fluxs)


  lazy val active = tab.map(t=>t==name)


  override def newItemView(item: Item): ItemView = this.constructItemView(item){
    case (el, _) =>
      println(s"hitsview added ${item.name}")
      new HitsView(el, item).withBinder(v => new CodeBinder(v))
  }

  lazy val s = dom.document.getElementById("canvas") match {
    case s: SVG => s
    case _ => throw new Exception("cannot find SVG canvas from FluxView")
  }

  override lazy val injector = defaultInjector
    .register("FluxGraphView") { case (el, args) =>
      new FluxGraphView(el, items,
        KappaNodeVisualSettings(12, 2),
        KappaEdgeVisualSettings(8, 1,
          LineParams(Colors.green)),
        s).withBinder(v => new CodeBinder(v))
    }
}