package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionSortedMapView}
import org.denigma.kappa.messages.ProjectRequests.GetList
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.circuits.NotebookCircuit
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

class ProjectsView(val elem: Element, val circuit: NotebookCircuit)
  extends CollectionSortedMapView {

  type Key = String

  type Value = KappaProject

  def updateView(view: ItemView, key: Key, old: Value, current: Value): Unit = {
    view.project() = current
  }

  def newItemView(key: Key, proj: Value): ItemView =  constructItemView(key){
    case (el, _) => new ProjectTitleView(el, Var(proj), circuit.selectedProject, circuit.output).withBinder(v=>new GeneralBinder(v))
  }

  lazy val items = circuit.allProjects

  val newProjectName = Var("")

  override type ItemView = ProjectTitleView

  val createProjectClick: Var[MouseEvent] = Var(Events.createMouseEvent())

  createProjectClick.onChange{ ev=>
      circuit.output() = ProjectRequests.Create(KappaProject(newProjectName.now), rewriteIfExists = false)
      newProjectName() = ""
  }

}

class ProjectTitleView(val elem: Element, val project: Var[KappaProject], val selectProject: Var[KappaProject], toServer: Var[KappaMessage]) extends BindableView {

  val name= project.map(p=>p.name)

  val openClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  openClick.triggerLater{
    selectProject() = project.now
  }

  val removeClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  removeClick.triggerLater{
    val message = s"Do you really want to remove '${project.now.name}' project?"
    val confirmation = dom.window.confirm(message)
    if(confirmation) toServer() = KappaMessage.Container().andThen(ProjectRequests.Remove(project.now.name)).andThen(GetList)
  }

  val downloadClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  downloadClick.triggerLater{
    toServer() = ProjectRequests.Download(project.now.name)
  }


  val current: Rx[Boolean] = Rx{
    project() == selectProject()
  }



}