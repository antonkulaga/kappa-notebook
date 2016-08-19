package org.denigma.kappa.notebook.views.project

import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.actions.Movements
import org.scalajs.dom.raw.Element
import rx._


class ProjectsPanelView(val elem: Element,
                        val currentProject: Var[KappaProject],
                        val input: Var[KappaMessage],
                        val output: Var[KappaMessage],
                        movements: Movements
                       ) extends BindableView {

  override lazy val injector = defaultInjector
    .register("ProjectsView")((el, args) => new ProjectsView(el, input, output).withBinder(n => new CodeBinder(n)))
    .register("CurrentProjectView")((el, args) =>
      new CurrentProjectView(el, currentProject, input, output, args.getOrElse("uploader", "fileUpload").toString, movements)
        .withBinder(n => new CodeBinder(n))
    )
}
