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
                        val currentProject: Var[CurrentProject],
                        val input: Var[KappaMessage],
                        val output: Var[KappaMessage]
                       ) extends BindableView {

  val isSaved = currentProject.map(p => p.saved)

  override lazy val injector = defaultInjector
    .register("ProjectsView")((el, args) => new ProjectsView(el, input, output).withBinder(n => new CodeBinder(n)))
    .register("CurrentProjectView")((el, args) => new CurrentProjectView(el, currentProject, input, output).withBinder(n => new CodeBinder(n)))
}
