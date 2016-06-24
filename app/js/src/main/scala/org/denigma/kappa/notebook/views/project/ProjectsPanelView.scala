package org.denigma.kappa.notebook.views.project

import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.editor.KappaWatcher
import org.denigma.kappa.notebook.views.visual.rules.GraphView
import org.scalajs.dom.raw.Element
import rx.Rx.Dynamic
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.collection.immutable.SortedSet


class ProjectsPanelView(val elem: Element,
                        val loaded: Rx[ProjectResponses.Loaded],
                        val currentProject: Var[CurrentProject],
                        //val currentProject: Var[KappaProject],
                        val input: Var[KappaMessage],
                        val output: Var[KappaMessage]
                       ) extends BindableView {

  loaded.foreach {
    case l if l.projectOpt.isDefined =>
      currentProject() = CurrentProject.fromKappaProject(l.projectOpt.get)
    case other =>
  }

  val isSaved = currentProject.map(p => p.saved)

  val projectList: Rx[SortedSet[KappaProject]] = loaded.map(l => l.projects)

  override lazy val injector = defaultInjector
    .register("ProjectsView")((el, args) => new ProjectsView(el, loaded, output).withBinder(n => new CodeBinder(n)))
    .register("CurrentProjectView")((el, args) => new CurrentProjectView(el, currentProject, input, output).withBinder(n => new CodeBinder(n)))
}
