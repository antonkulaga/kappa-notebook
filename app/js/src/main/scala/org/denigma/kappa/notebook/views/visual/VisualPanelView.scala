package org.denigma.kappa.notebook.views.visual

import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.views.editor.KappaWatcher
import org.denigma.kappa.notebook.views.visual.rules.{GraphView, VisualSettings}
import org.denigma.kappa.notebook.views.visual.utils.LineParams
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable.SortedSet

class VisualPanelView(val elem: Element, kappaWatcher: KappaWatcher, input: Var[KappaMessage], s: SVG) extends BindableView{

  val currentLine: Rx[String] = kappaWatcher.text

  val selected: Var[String] = Var("rules")//Var("contact_map")

  val rulesActive = selected.map(s=>s=="rules")
  val contactActive = selected.map(s=>s=="contact_map")

  //val headers = itemViews.map(its=>SortedSet.empty[String] ++ its.values.map(_.id))

  override lazy val injector = defaultInjector
    .register("ContactMapView") {
      (el, args) =>new ContactMapView(el, input).withBinder(v=>new CodeBinder(v))
    }
    /*
    .register("LeftGraph") {  (el, args) =>
      new GraphView(el,
        kappaWatcher.leftPattern.nodes,
        kappaWatcher.leftPattern.edges,
        kappaWatcher.leftPattern.layouts,
        args.getOrElse("container", "graph-container").toString).withBinder(n => new CodeBinder(n)) }
    .register("RightGraph") {  (el, args) =>
      new GraphView(el,
        kappaWatcher.rightPattern.nodes,
        kappaWatcher.rightPattern.edges,
        kappaWatcher.rightPattern.layouts,
        args.getOrElse("container", "graph-container").toString).withBinder(n => new CodeBinder(n)) }
    */
    .register("LeftGraph") {  (el, args) =>
    new GraphView(el,
      kappaWatcher.leftPattern.map(p=>SortedSet(p.agents:_*)),
      args.getOrElse("container", "graph-container").toString,
      VisualSettings(s)).withBinder(n => new CodeBinder(n)) }
    .register("RightGraph") {  (el, args) =>
      new GraphView(el,
        kappaWatcher.rightPattern.map(p=>SortedSet(p.agents:_*)),
        args.getOrElse("container", "graph-container").toString, VisualSettings(s)).withBinder(n => new CodeBinder(n)) }

}
