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
import org.denigma.binding.extensions._
import org.scalajs.dom

import scala.collection.immutable._


class KappaCodeEditor(val elem: Element,
                      val items: Var[Map[String, KappaFile]],
                      val selector: Selector,
                      val errorsList: Var[List[String]],
                      val kappaCursor: Var[Option[(Editor, PositionLike)]],
                      val editorUpdates: Var[EditorUpdates]) extends BindableView
  with ItemsMapView
{

  //items.foreach(i=>println(" FILES: "+i))

  override type Item = String

  val selected: Var[String] = Var("")

  val errors: Rx[String] = errorsList.map(er=> if(er.isEmpty) "" else er.reduce(_ + "\n" + _))

  val hasErrors = errors.map(e=>e != "")

  val headers = itemViews.map(its=> SortedSet.empty[String] ++ its.values.map(_.id))

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selector.source).withBinder(new GeneralBinder(_)))

  override type Value = KappaFile

  override type ItemView = CodeTab

  protected def keyVar(key: Key) = {
    require(items.now.contains(key), s"we are adding an Item view for key(${key}) that does not exist")
    val initialValue = this.items.now(key)
    val v = Var(initialValue)
    v.onChange{
      case value=>
        items() = items.now.updated(key, value)
    }
    v
    //note: killing should be done on unbinding
  }

  override def newItemView(item: Item): ItemView = this.constructItemView(item) {
    case (el, _) =>
      el.id = item //dirty trick
      val value = keyVar(item)
      val view: ItemView = new CodeTab(el, item, value, selected, editorUpdates, kappaCursor).withBinder(v => new CodeBinder(v) )
      selected() = item
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
}
