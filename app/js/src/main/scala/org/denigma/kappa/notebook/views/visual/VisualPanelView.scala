package org.denigma.kappa.notebook.views.visual

import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.views.common.FixedBinder
import org.denigma.kappa.notebook.views.visual.rules._
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.parsers.{GraphUpdate, ParsedLine}
import rx._

import scala.collection.immutable._

object ShowParameters extends Enumeration {
  val Removed, Added, Unchanged, Updated = Value
}

class VisualPanelView(val elem: Element, val currentLine: Rx[String], val parsed: Rx[ParsedLine], input: Var[KappaMessage], s: SVG) extends BindableView{

  val selected: Var[String] = Var("rules")//Var("contact_map")

  val rulesActive: Rx[Boolean] = selected.map(s=>s=="rules")
  val contactActive = selected.map(s=>s=="contact_map")

  val left: Var[Boolean] = Var(false)
  val right: Var[Boolean] = Var(false)
  val both: Var[Boolean] = Var(true)

  left.onChange(v => dom.console.log("LEFT IS "+v))
  right.onChange{
    v =>
      val msg = s"RIGHT IS ${v}"
      dom.window.alert(msg)
  }
  both.onChange(v => dom.console.log("BOTH IS "+v))


  val update: Rx[GraphUpdate] = parsed.map{ p => GraphUpdate.fromParsedLine(p)}

  lazy val isRule = update.map(_.isRule)

  import KappaModel._
  val sameAgents: Rx[List[(Agent, Agent)]] = update.map(u=>u.sameAgents)

  val unchangedAgents: Rx[Set[Agent]] = update.map(u=>u.unchangedAgents)

  val updatedAgents: Rx[Set[(Agent, Agent)]] = update.map(u => u.updatedAgents)

  val modifiedAgents: Rx[(List[Agent], List[Agent])] = update.map(u => u.modifiedAgents)

  val leftModified = update.map(u => u.leftModified)
  val rightModified =  update.map(u => u.rightModified)

  val removedAgents: Rx[Set[Agent]] = update.map(u => u.removedAgents)

  val addedAgents: Rx[Set[Agent]] =  update.map(u => u.addedAgents)



  override lazy val injector = defaultInjector
        .register("ContactMapView") {
          (el, args) =>new ContactMapView(el, input, contactActive).withBinder(v=>new CodeBinder(v))
        }
        .register("WholeGraph") {  (el, args) =>
        //2 in one
        new WholeRuleGraphView(el,
          unchangedAgents,
          removedAgents,
          addedAgents,
          updatedAgents,
          args.getOrElse("container", "whole-graph-container").toString, RulesVisualSettings(s)).withBinder(n => new FixedBinder(n)) }
}
