package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.controls.pdf.{PDFPageViewport, TextLayerBuilder, TextLayerOptions}
import org.querki.jquery.$
import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsMapView, ItemsSeqView, UpdatableView}
import org.denigma.kappa.notebook.KappaHub
import org.denigma.kappa.notebook.views.common.TabItem
import org.denigma.kappa.notebook.views.simulations.TabHeaders

import scala.annotation.tailrec
import scala.collection.immutable
import scala.scalajs.js
import scala.util.{Failure, Success}
import scalajs.concurrent.JSExecutionContext.Implicits.queue

class PapersView(val elem: Element, val selected: Var[String], hub: KappaHub) extends
  BindableView
  with ItemsMapView
  with TabItem{

  override type Item = String

  override type Value = Bookmark

  override type ItemView = PublicationView

  val items: Var[Map[String, Bookmark]] = hub.papers

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  override def newItemView(name: String): PublicationView = this.constructItemView(name){
    case (el, params)=>
      el.id = name
      //println("add view "+name)
      val location: Bookmark = this.items.now(name) //buggy but hope it will work
      val v = new PublicationView(el, hub.selector.paper, Var(location), hub ).withBinder(v=>new CodeBinder(v))
      hub.selector.paper() = name
      v
  }


  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, hub.selector.paper).withBinder(new GeneralBinder(_)))

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


