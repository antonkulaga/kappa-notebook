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

  val selectTab = Var("")

  override type Item = String

  override type Value = Bookmark

  override type ItemView = PublicationView

  val items = hub.papers

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  override def newItemView(name: String): PublicationView = this.constructItemView(name){
    case (el, params)=>
      el.id = name
      println("add view "+name)
      val location = this.items.now(name) //buggy but hope it will work
      val v = new PublicationView(el, selectTab, Var(location), hub ).withBinder(v=>new CodeBinder(v))
      selectTab() = name
      v
  }


  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selectTab).withBinder(new GeneralBinder(_)))

  override protected def subscribeUpdates() = {
    template.hide()
    //this.items.now.foreach(i => this.addItemView(i, this.newItemView(i)) ) //initialization of views
    updates.onChange(upd=>{
      upd.added.foreach{
        case (key, value)=>
          val n = newItemView(key)
          n.update(value)
          this.addItemView(key, n)
      }
      upd.removed.foreach{ case (key, value ) => removeItemView(key)}
      upd.updated.foreach{ case( key, (old, current))=> itemViews.now(key).update(current)}
    })
    //TODO: move to scalajs binding
    for ( (key, value) <- items.now) {
      val n = newItemView(key)
      n.update(value)
      this.addItemView(key, n)
    }
  }

}


