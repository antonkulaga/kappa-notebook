package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsMapView}
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
                 val location: Var[Bookmark],
                 val currentProjectName: Rx[String],
                 val connector: WebSocketTransport,
                 val kappaCursor: Var[Option[(Editor, PositionLike)]]) extends BindableView
  with ItemsMapView {

  val paperLoader: WebSocketPaperLoader = WebSocketPaperLoader(connector, projectName = currentProjectName)

  val items: Var[Map[String, Paper]] = paperLoader.loadedPapers

  val empty = items.map(its=>its.isEmpty)

  override type Item = String

  override type Value = Paper//Bookmark

  override type ItemView = PublicationView

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  location.foreach{
    case loc if loc.paper!="" =>
      println("LOAD :"+loc.paper)
      paperLoader.getPaper(loc.paper, 10 seconds).onComplete{
        case Success(pp) =>
          paperLoader.loadedPapers() = paperLoader.loadedPapers.now.updated(pp.name, pp)
          println("test")

          //selected() = pp.name
        case Failure(th)=> dom.console.error(s"Cannot load paper ${loc.paper}: "+th)
      }
    case _ => //do nothing
  }

  val selected = Var("")

  connector.input.onChange {
    case GoToPaper(loc)=> location() = loc
    case other => //do nothing
  }

  override def newItemView(name: String): PublicationView = this.constructItemView(name){
    case (el, params)=>
      el.id = name
      val paper: Paper = this.items.now(name) //buggy but hope it will work
      val v = new PublicationView(el, location, paper, kappaCursor).withBinder(v=>new CodeBinder(v))
      v
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected).withBinder(new GeneralBinder(_)))
    .register("Bookmarks")((el, args) => new BookmarksView(el, location, null).withBinder(new GeneralBinder(_)))

}


