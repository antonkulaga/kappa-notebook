package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsMapView}
import org.denigma.codemirror._
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.{KappaFile, KappaProject}
import org.denigma.kappa.notebook.Selector
import org.denigma.kappa.notebook.views.simulations.TabHeaders
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._


class KappaCodeEditor(val elem: Element,
                      val currentProject: Var[KappaProject],
                      val selector: Selector,
                      val errorsList: Var[List[String]],
                      val kappaCursor: Var[Option[(Editor, PositionLike)]],
                      val editorUpdates: Var[EditorUpdates]) extends BindableView
  with ItemsMapView
{

  val selected: Var[String] = Var("")

  val errors: Rx[String] = errorsList.map(er=> if(er.isEmpty) "" else er.reduce(_ + "\n" + _))

  val hasErrors = errors.map(e=>e != "")

  val headers = itemViews.map(its=> SortedSet.empty[String] ++ its.values.map(_.id))

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selector.source).withBinder(new GeneralBinder(_)))
    //.register("code")((el, args) => new CodeTab(el, hub.selector.source).withBinder(new GeneralBinder(_))

  /*
    override def withBinder(fun: this.type => ViewBinder): this.type  = withBinders(fun(this)::binders)

    override def withBinders(fun: this.type => List[ViewBinder]): this.type  = withBinders(fun(this) ++ binders)
    */
  override type Value = KappaFile

  override def items: Rx[Map[String, KappaFile]] = currentProject.map(p=>p.sources)

  override type ItemView = CodeTab

  override def newItemView(item: Item): ItemView = this.constructItemView(item) {
    case (el, _) =>
      el.id = item
      val value = this.items.now(item) //buggy but hope it will work
      val view = new CodeTab(el, item, Var(value), selected, editorUpdates, kappaCursor).withBinder(v=>new CodeBinder(v))
      selected() = item
      //view.code() = va
      view

  }

  override protected def subscribeUpdates() = {
    super.subscribeUpdates()
    //TODO: move to scalajs binding
    for ( (key, value) <- items.now) {
      val n = newItemView(key)
      n.update(value)
      this.addItemView(key, n)
    }
  }


  override type Item = String
}
