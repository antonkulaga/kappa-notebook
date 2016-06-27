package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, ItemsSeqView, ItemsSetView}
import org.denigma.kappa.messages._
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._

import scala.collection.immutable.{Seq, SortedSet}

class ProjectsView(val elem: Element,
                   val input: Var[KappaMessage],
                   val sender: Var[KappaMessage]) extends ItemsSetView {

  val items: Var[SortedSet[KappaProject]] = Var(SortedSet.empty[KappaProject])

  val selectedProject = Var(KappaProject.empty)
  selectedProject.onChange{
    case proj => sender() = ProjectRequests.Load(proj)
  }

  input.onChange{

    case ProjectResponses.LoadedProject(proj) =>
      selectedProject() = proj

    case ld: ProjectResponses.ProjectList =>
      //println("LOQDED = "+ld)
      items() = SortedSet(ld.projects:_*)

    case org.denigma.kappa.messages.Done(cr: ProjectRequests.Create, _) =>
      println("project has been created, loading it...")
      sender() = ProjectRequests.Load(cr.project)

    case _=> //do nothing
  }

  val newProjectName = Var("")

  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectTitleView(el, item, selectedProject).withBinder(v=>new GeneralBinder(v))
  }

  override type Item = KappaProject
  override type ItemView = ProjectTitleView

  val createProjectClick: Var[MouseEvent] = Var(Events.createMouseEvent())

  createProjectClick.onChange{
    case ev=>
      sender() = ProjectRequests.Create(KappaProject(newProjectName.now), false)
  }

}

class ProjectTitleView(val elem: Element, val project: KappaProject, val selectProject: Var[KappaProject]) extends BindableView {

  val name: Var[String] = Var(project.name)

  val loadClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  loadClick.triggerLater{
    selectProject() = project
  }

  val current: Rx[Boolean] = selectProject.map(p=>p==project)


}