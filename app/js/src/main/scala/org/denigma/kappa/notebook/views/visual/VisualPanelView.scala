package org.denigma.kappa.notebook.views.visual

import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.parsers.{GraphUpdate, ParsedLine}
import org.denigma.kappa.notebook.views.visual.ShowParameters.ShowParameters
import org.denigma.kappa.notebook.views.visual.rules._
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

object ShowParameters extends Enumeration {
  type ShowParameters = Value
  val Left, Right, Both = Value
}

class VisualPanelView(val elem: Element, val currentLine: Rx[String], val parsed: Rx[ParsedLine], input: Var[KappaMessage], s: SVG) extends BindableView{

  val selected: Var[String] = Var("rules")//Var("contact_map")

  val showState: Var[ShowParameters] = Var(ShowParameters.Both)
  showState.onChange //UGFLY PART TODO: fix it
  {
    case ShowParameters.Left =>
      right.Internal.value = false
      left.Internal.value = true
      both.Internal.value = false

    case ShowParameters.Right =>
      right.Internal.value = true
      left.Internal.value = false
      both.Internal.value = false

    case ShowParameters.Both =>
      right.Internal.value = false
      left.Internal.value = false
      both.Internal.value = true
  }

  val rulesActive: Rx[Boolean] = selected.map(s=>s=="rules")
  val contactActive = selected.map(s=>s=="contact_map")

  val left: Var[Boolean] = Var(false)
  val right: Var[Boolean] = Var(false)
  val both: Var[Boolean] = Var(true)

  left.onChange(v => if(v) showState() = ShowParameters.Left)
  right.onChange{v =>if(v)  showState() = ShowParameters.Right}
  both.onChange(v => if(v) showState() = ShowParameters.Both)


  val update: Rx[GraphUpdate] = parsed.map{ p => GraphUpdate.fromParsedLine(p)}

  lazy val isRule = update.map(_.isRule)
  lazy val updateInfo = update.map(_.updateInfo)

  override lazy val injector = defaultInjector
      .register("ContactMapView") {
        (el, args) =>new ContactMapView(el, input, contactActive).withBinder(v=>new CodeBinder(v))
      }
      .register("WholeGraph") {  (el, args) =>
      //2 in one
      new WholeRuleGraphView(el,
        updateInfo, showState,
        args.getOrElse("container", "whole-graph-container").toString,
        RulesVisualSettings(s),
        input
      ).withBinder(n => new  CodeBinder(n)) }
}
