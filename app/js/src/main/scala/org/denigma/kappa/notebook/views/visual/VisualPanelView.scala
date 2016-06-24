package org.denigma.kappa.notebook.views.visual

import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.views.editor.KappaWatcher
import org.denigma.kappa.notebook.views.project.ProjectsView
import org.denigma.kappa.notebook.views.visual.rules.GraphView
import org.scalajs.dom.raw.Element
import rx._

class VisualPanelView(val elem: Element, kappaWatcher: KappaWatcher, input: Var[KappaMessage]) extends BindableView{

  val currentLine: Rx[String] = kappaWatcher.text

  override lazy val injector = defaultInjector
    .register("ContactMapView") {
      (el, args) =>new ContactMapView(el, input).withBinder(v=>new CodeBinder(v))
    }
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

}