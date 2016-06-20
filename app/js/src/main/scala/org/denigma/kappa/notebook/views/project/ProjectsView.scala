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

class ProjectsView(val elem: Element, val loaded: Rx[ProjectResponses.Loaded], val sender: Var[KappaMessage]) extends ItemsSetView {

  val selectProject = Var(loaded.now.projectOpt.getOrElse(KappaProject.default))
  selectProject.onChange{
    case proj =>
      sender() = ProjectRequests.Load(proj)
  }

  val currentProject = loaded.map{
    case lds=>
      val p = lds.projectOpt.getOrElse(KappaProject.default)
      selectProject() = p
      p
  }

  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectTitleView(el, item, selectProject).withBinder(v=>new GeneralBinder(v))
  }

  override type Item = KappaProject
  override type ItemView = ProjectTitleView
  override val items: Rx[SortedSet[KappaProject]] = loaded.map(lds=>lds.projects)

  val createProjectClick: Var[MouseEvent] = Var(Events.createMouseEvent())
}

class ProjectTitleView(val elem: Element, val project: KappaProject, val selectProject: Var[KappaProject]) extends BindableView {

  val name: Var[String] = Var(project.name)

  val loadClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  loadClick.triggerLater{
    selectProject() = project
  }

  val current: Rx[Boolean] = selectProject.map(p=>p==project)


}