package org.denigma.kappa.notebook.views.project

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsSeqView}
import org.denigma.kappa.messages.KappaProject
import org.denigma.kappa.notebook.KappaHub
import org.scalajs.dom.raw.Element
import rx._

import scala.collection.immutable.Seq

class ProjectsView(val elem: Element, val projectList: Rx[List[KappaProject]]) extends ItemsSeqView {


  override def newItemView(item: Item): ItemView = constructItemView(item){
    case (el, _) => new ProjectTitleView(el,  Var(item.name)).withBinder(v=>new GeneralBinder(v))
  }

  override type Item = KappaProject
  override type ItemView = ProjectTitleView
  override val items: Rx[Seq[KappaProject]] = projectList
}

class ProjectTitleView(val elem: Element, val name: Var[String]) extends BindableView {

}