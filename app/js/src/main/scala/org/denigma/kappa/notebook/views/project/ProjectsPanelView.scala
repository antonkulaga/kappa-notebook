package org.denigma.kappa.notebook.views.project

import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.editor.KappaWatcher
import org.denigma.kappa.notebook.views.visual.GraphView
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.collection.immutable.SortedSet


class ProjectsPanelView(val elem: Element,
                        val sourceMap: Var[Map[String, KappaFile]],
                        val loaded: Rx[ProjectResponses.Loaded],
                        val input: Var[KappaMessage],
                        val output: Var[KappaMessage],
                        val kappaWatcher: KappaWatcher
                       ) extends BindableView
{

  val currentProject = loaded.map{
    case l if l.projectOpt.isDefined=>
      l.projectOpt.get
    case other=>
      val proj = KappaProject.default
      proj
  }

  val isSaved = currentProject.map(p=>p.saved)

  val projectList: Rx[SortedSet[KappaProject]] = loaded.map(l=>l.projects)

  val currentLine: Rx[String] = kappaWatcher.text


  override lazy val injector = defaultInjector
  .register("ProjectsView")((el, args) => new ProjectsView(el, loaded, output).withBinder(n => new CodeBinder(n)))
  .register("ProjectFilesView")((el, args) => new CurrentProjectView(el, currentProject, sourceMap, input, output).withBinder(n => new CodeBinder(n)))
  .register("LeftGraph") {  (el, args) =>
    new GraphView(el,
      kappaWatcher.leftPattern.nodes,
      kappaWatcher.leftPattern.edges,
      kappaWatcher.leftPattern.layouts,
      args.getOrElse("container","graph-container").toString).withBinder(n => new CodeBinder(n)) }
  .register("RightGraph") {  (el, args) =>
    new GraphView(el,
      kappaWatcher.rightPattern.nodes,
      kappaWatcher.rightPattern.edges,
      kappaWatcher.rightPattern.layouts,
      args.getOrElse("container","graph-container").toString).withBinder(n => new CodeBinder(n)) }

}
