package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, CollectionMapView}
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.messages.GoToPaper
import org.denigma.kappa.notebook._
import org.denigma.kappa.notebook.views.common.TabHeaders
import org.scalajs.dom
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.denigma.binding.extensions._

import scala.collection.immutable
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

class PapersView(val elem: Element,
                 val currentProjectName: Rx[String],
                 val connector: WebSocketTransport,
                 val kappaCursor: Var[Option[(Editor, PositionLike)]]) extends BindableView
  with CollectionMapView {

  val paperURI: Var[String] = Var("")

  val paperLoader: WebSocketPaperLoader = WebSocketPaperLoader(connector, projectName = currentProjectName)

  val items: Var[Map[String, Paper]] = paperLoader.loadedPapers

  val empty = items.map(its=>its.isEmpty)

  override type Key = String

  override type Value = Paper

  override type ItemView = PublicationView

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  paperURI.foreach{
    case paper if paper!="" =>
      //println("LOAD :"+loc.paper)
      paperLoader.getPaper(paper, 10 seconds).onComplete{
        case Success(pp) =>
          paperLoader.loadedPapers() = paperLoader.loadedPapers.now.updated(pp.name, pp)
          println("test")

          //selected() = pp.name
        case Failure(th)=> dom.console.error(s"Cannot load paper ${paper}: "+th)
      }
    case _ => //do nothing
  }

  val selected = Var("")

  connector.input.onChange {
    case GoToPaper(loc)=> paperURI() = loc
    case other => //do nothing
  }

  lazy val lineNumber = kappaCursor.map{c => c.map(p => p._2.line).getOrElse(0)}
  lazy val charNumber = kappaCursor.map{c => c.map(p => p._2.ch).getOrElse(0)}

  val canInsert: Rx[Boolean] = kappaCursor.map(c => c.isDefined)
  val comment = Var("")
  val insertComment = Var(Events.createMouseEvent())
  insertComment.triggerLater{
    dom.console.info("INSERTION WORKS!")
  }

  override def newItemView(name: String, paper: Paper): PublicationView = this.constructItemView(name){
    case (el, params)=>
      el.id = name
      val v = new PublicationView(el, paperURI, comment, Var(paper), kappaCursor).withBinder(v=>new CodeBinder(v))
      v
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected).withBinder(new GeneralBinder(_)))
    //.register("Bookmarks")((el, args) => new BookmarksView(el, location, null).withBinder(new GeneralBinder(_)))

  override def updateView(view: PublicationView, key: String, old: Paper, current: Paper): Unit = {
    view.paper() = current
  }
}


