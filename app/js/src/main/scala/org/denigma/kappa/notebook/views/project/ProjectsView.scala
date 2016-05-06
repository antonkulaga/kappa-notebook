package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, ItemsSeqView}
import org.denigma.kappa.messages.KappaProject
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.Element
import rx.Rx.Dynamic
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.collection.immutable.Seq

class ProjectsView(val elem: Element, val projectList: Rx[List[KappaProject]]) extends ItemsSeqView {

  //val selectedProject: Var[KappaProject]

  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectTitleView(el, item/*, selectedProject*/).withBinder(v=>new GeneralBinder(v))
  }

  override type Item = KappaProject
  override type ItemView = ProjectTitleView
  override val items: Rx[Seq[KappaProject]] = projectList

}

class ProjectTitleView(val elem: Element, val project: KappaProject/*, val selectedProject: Var[KappaProject]*/) extends BindableView {

  val name: Var[String] = Var(project.name)

  val loadClick: Var[MouseEvent] = Var(Events.createMouseEvent())
  loadClick.triggerLater{
    //selectedProject() = project
  }

  val current: Rx[Boolean] = Var(true)//selectedProject.map(p=>p==project)
}