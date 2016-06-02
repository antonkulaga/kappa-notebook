package org.denigma.kappa.notebook.views.annotations.papers

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsMapView}
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.notebook.views.annotations.AnnotatorNLP
import org.denigma.kappa.notebook.{Selector, WebSocketTransport}
import org.denigma.kappa.notebook.views.common.TabItem
import org.denigma.kappa.notebook.views.simulations.TabHeaders
import org.denigma.nlp.communication.WebSocketNLPTransport
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import scala.concurrent.duration._

import scala.collection.immutable
import scala.collection.immutable.SortedSet

class PapersView(val elem: Element,
                 val selected: Var[String],
                 val paperLoader: PaperLoader,
                 val selector: Selector,
                 val kappaCursor: Var[Option[(Editor, PositionLike)]]) extends
  BindableView
  with ItemsMapView
  with TabItem{

  val items: Var[Map[String, Paper]] = paperLoader.loadedPapers

  override type Item = String

  override type Value = Paper//Bookmark

  override type ItemView = PublicationView

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  override def newItemView(name: String): PublicationView = this.constructItemView(name){
    case (el, params)=>
      el.id = name
      //println("add view "+name)
      val paper: Paper = this.items.now(name) //buggy but hope it will work
      val v = new PublicationView(el, selector.paper, paper, kappaCursor).withBinder(v=>new CodeBinder(v))
      selector.paper() = name
      v
  }

  lazy val transportNLP = WebSocketNLPTransport("localhost:1112","notebook", "guest" + Math.random() * 1000)


  override def bindView() = {
    super.bindView()
    transportNLP.open()
  }



  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selector.paper).withBinder(new GeneralBinder(_)))
    .register("annotator")((el, args) => new AnnotatorNLP(el, transportNLP).withBinder(new GeneralBinder(_)))

}


