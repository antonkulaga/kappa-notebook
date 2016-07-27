package org.denigma.kappa.notebook.views.visual

import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.views.editor.KappaWatcher
import org.denigma.kappa.notebook.views.visual.rules.{RulesGraphView, RulesVisualSettings}
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._

class VisualPanelView(val elem: Element, kappaWatcher: KappaWatcher, input: Var[KappaMessage], s: SVG) extends BindableView{

  val currentLine: Rx[String] = kappaWatcher.text

  val selected: Var[String] = Var("rules")//Var("contact_map")

  val rulesActive = selected.map(s=>s=="rules")
  val contactActive = selected.map(s=>s=="contact_map")

  //val headers = itemViews.map(its=>SortedSet.empty[String] ++ its.values.map(_.id))

  //val leftAgents = kappaWatcher.leftPattern.map(p=>SortedSet(p.agents:_*))
  //val rightAgents = kappaWatcher.rightPattern.map(p=>SortedSet(p.agents:_*)

  //val unchanged: Rx[Set[Agent]],
  //val removed: Rx[Set[Agent]],
  //val added: Rx[Set[Agent]],
  //val updated: Rx[Set[Agent]],


  override lazy val injector = defaultInjector
    .register("ContactMapView") {
      (el, args) =>new ContactMapView(el, input, contactActive).withBinder(v=>new CodeBinder(v))
    }
    .register("LeftGraph") {
      (el, args) =>
    new RulesGraphView(el,
      kappaWatcher.leftUnchanged,
      kappaWatcher.removed,
      Var(Set.empty[KappaModel.Agent]),
      kappaWatcher.leftModified,
      args.getOrElse("container", "graph-container").toString,
      RulesVisualSettings(s)).withBinder(n => new CodeBinder(n)) }
    .register("RightGraph") {  (el, args) =>
      new RulesGraphView(el,
        kappaWatcher.rightUnchanged,
        Var(Set.empty[KappaModel.Agent]),
        kappaWatcher.added,
        kappaWatcher.rightModified,
        args.getOrElse("container", "graph-container").toString, RulesVisualSettings(s)).withBinder(n => new CodeBinder(n)) }

}
