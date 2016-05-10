package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, ItemsSeqView, ItemsSetView}
import org.denigma.kappa.messages.FileResponses.Loaded
import org.denigma.kappa.messages.{FileRequests, KappaMessage, KappaProject}
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.Element
import rx.Rx.Dynamic
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._

import scala.collection.immutable.{Seq, SortedSet}

class ProjectsView(val elem: Element, val loaded: Rx[Loaded], val sender: Var[KappaMessage]) extends ItemsSetView {

  val selectProject = Var(loaded.now.project)
  selectProject.onChange{
    case proj =>
      sender() = FileRequests.Load(proj)
  }

  val currentProject = loaded.map{
    case lds=>
      selectProject() = lds.project
      lds.project
  }

  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectTitleView(el, item, selectProject).withBinder(v=>new GeneralBinder(v))
  }

  override type Item = KappaProject
  override type ItemView = ProjectTitleView
  override val items: Rx[SortedSet[KappaProject]] = loaded.map(lds=>lds.projects)


}

class ProjectTitleView(val elem: Element, val project: KappaProject, val selectProject: Var[KappaProject]) extends BindableView {

  val name: Var[String] = Var(project.name)

  val loadClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  loadClick.triggerLater{
    selectProject() = project
  }

  val current: Rx[Boolean] = selectProject.map(p=>p==project)
}