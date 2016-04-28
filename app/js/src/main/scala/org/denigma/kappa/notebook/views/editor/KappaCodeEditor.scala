package org.denigma.kappa.notebook.views.editor

import fastparse.all._
import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsMapView}
import org.denigma.codemirror._
import org.denigma.codemirror.extensions._
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaFile
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.views.simulations.TabHeaders
import org.scalajs.dom.html._
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx._
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.util._
import scala.scalajs.js
import scalatags.JsDom.all._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.kappa.notebook.views.common.TabItem
import rx.Rx.Dynamic

import scala.Predef.Map
import scala.collection.immutable._


/**
  * Editor for the kappa code
 *
  * @param elem HTML element for the view
  * @param hub ugly shareble hub to connect with other UI elements
  * @param editorUpdates reactive varible to which we report our editor updates
  */
class KappaCodeEditor(val elem: Element, val hub: KappaHub, val editorUpdates: Var[EditorUpdates]) extends BindableView
  with ItemsMapView
{

  val selected: Var[String] = Var("")

  val errors: Rx[String] = hub.errors.map(er=> if(er.isEmpty) "" else er.reduce(_ + "\n" + _))

  val hasErrors = errors.map(e=>e != "")

  //def code = hub.kappaCode

  //override def mode = "Kappa"


  val headers = itemViews.map(its=> SortedSet.empty[String] ++ its.values.map(_.id))



  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, hub.selector.source).withBinder(new GeneralBinder(_)))
    //.register("code")((el, args) => new CodeTab(el, hub.selector.source).withBinder(new GeneralBinder(_))

  /*
    override def withBinder(fun: this.type => ViewBinder): this.type  = withBinders(fun(this)::binders)

    override def withBinders(fun: this.type => List[ViewBinder]): this.type  = withBinders(fun(this) ++ binders)
    */
  override type Value = KappaFile

  override def items: Rx[Map[String, KappaFile]] = hub.sources.map{
    case src =>
      //dom.console.log("sources are : "+ src )
      src.map(f=> (f.name , f)).toMap
  }

  override type ItemView = CodeTab

  override def newItemView(item: Item): ItemView = this.constructItemView(item) {
    case (el, _) =>
      el.id = item
      val value = this.items.now(item) //buggy but hope it will work
      val view = new CodeTab(el, item, Var(value), selected, editorUpdates, hub.kappaCursor).withBinder(v=>new CodeBinder(v))
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


  override type Item = String
}
