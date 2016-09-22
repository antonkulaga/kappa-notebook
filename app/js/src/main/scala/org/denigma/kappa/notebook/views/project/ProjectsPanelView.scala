package org.denigma.kappa.notebook.views.project

import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.notebook.circuits.{CurrentProjectCircuit, NotebookCircuit}
import org.scalajs.dom.raw.Element


class ProjectsPanelView(val elem: Element,
                        val circuit: NotebookCircuit
                       ) extends BindableView {

  override lazy val injector = defaultInjector
    .register("ProjectsView")((el, args) => new ProjectsView(el, circuit).withBinder(n => new CodeBinder(n)))
    .register("CurrentProjectView") { (el, args) =>
      val currentProjectCircuit = new CurrentProjectCircuit(circuit.input, circuit.output, circuit.currentProject)
      currentProjectCircuit.activate()
      new CurrentProjectView(el, currentProjectCircuit, args.getOrElse("uploader", "fileUpload").toString)
        .withBinder(n => new CodeBinder(n))
    }
}
