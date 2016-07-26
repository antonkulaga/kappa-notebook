package org.denigma.kappa.notebook.views.simulations.fluxes

import scala.collection.immutable._
import org.denigma.binding.views.{BindableView, ItemsSeqView, ItemsSetView, UpdatableView}
import org.denigma.kappa.messages.WebSimMessages.FluxMap
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.views.common.FixedBinder
import org.scalajs.dom
import rx.Rx.Dynamic





class HitsView(val elem: Element, ruleFlux: RuleFlux) extends BindableView {
  val rule = Var(ruleFlux.rule)
  val flux = Var(ruleFlux.flux.toString)
  val hits = Var(ruleFlux.hits)
}


object RuleFlux{
  implicit val ordering: Ordering[RuleFlux] = new Ordering[RuleFlux] {
    override def compare(x: RuleFlux, y: RuleFlux): Int = x.rule.compare(y.rule) match {
      case 0 =>
        x.begin.compare(y.begin) match {
          case 0 =>
            x.end.compare(y.end) match {
              case 0 => x.hashCode().compare(y.hashCode())
              case other => other
            }
          case other => other
        }
      case other => other
    }
  }

  def fromFluxMap(fluxMap: FluxMap): SortedSet[RuleFlux] = {
    if(fluxMap.flux_rules.length !=fluxMap.flux_hits.length) dom.console.error(s"length of rules and hits is different")
    if(fluxMap.flux_rules.length !=fluxMap.flux_fluxs.length) dom.console.error(s"length of rules and fluxs is different")
    val data: Map[String, (Int, List[Double])] = fluxMap.flux_rules.zip(fluxMap.flux_hits.zip(fluxMap.flux_fluxs)).toMap
    val list= data.map{ case (rule, (hits, fl)) =>
      val flux = fluxMap.flux_rules.zip(fl).toMap
      RuleFlux(rule, fluxMap.flux_name, fluxMap.flux_begin_time, fluxMap.flux_end_time, hits, flux)
    }.toList
    SortedSet(list:_*)
  }
}

case class RuleFlux(rule: String, name: String, begin: Double, end: Double, hits: Double, flux: Map[String, Double])
