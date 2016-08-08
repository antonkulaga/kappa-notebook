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

import scala.annotation.tailrec
import org.denigma.kappa.notebook.extensions._
import rx.Rx.Dynamic

import scala.collection.immutable
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

trait NodesChecker {

}

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
  val selectionOpt: Var[Option[Selection]] = Var(None)

  connector.input.onChange {
    case GoToPaper(loc)=> paperURI() = loc
    case other => //do nothing
  }

  lazy val lineNumber = kappaCursor.map{c => c.map(p => p._2.line).getOrElse(0)}
  lazy val charNumber = kappaCursor.map{c => c.map(p => p._2.ch).getOrElse(0)}

  val canInsert: Rx[Boolean] = kappaCursor.map(c => c.isDefined)
  val insertComment = Var(Events.createMouseEvent())
  insertComment.triggerLater{
    kappaCursor.now match {
      case Some((ed, pos)) =>
        val line = ed.getDoc().getLine(pos.line)
        ed.getDoc().setLine(pos.line, line + comment.now)
        println(s"POSITION = ${pos.line} ch is ${pos.ch} LEN IS ${line.length}")
      case None => dom.console.error("EDITOR IS NOT AVALIABLE TO INSERT")
    }
  }

  override protected def subscribeUpdates(): Unit =
  {
    super.subscribeUpdates()
    selectionOpt.afterLastChange(900 millis){
      case Some(sel) =>
         selections() = itemViews.now.map{ case (item, child) => (item, child.select(sel)) }

      case None =>
        selections() = Map.empty[Item, (Map[Int, List[TextLayerSelection]])]

    }
    dom.document.addEventListener("selectionchange", onSelectionChange _)


  }

  protected def onSelectionChange(event: Event) = {
    val selection: Selection = dom.window.getSelection()
    selection.anchorNode.isInside(elem) || selection.focusNode.isInside(elem)  match {
      case true =>
       selectionOpt() = Some(selection)
      case false =>
        selectionOpt() = None
      //println(s"something else ${selection.anchorNode.textContent}") //do nothing
    }
  }

  val selections: Var[Map[Item, (Map[Int, List[TextLayerSelection]])]] = Var(Map.empty[Item, (Map[Int, List[TextLayerSelection]])])

  protected def toURI(str: String) = if(str.contains(":")) str else ":"

  val comment: Rx[String] = selections.map{ chosen =>
    val result = chosen.foldLeft(""){
      case (acc, (item, mp))=>
        val sstr = mp.foldLeft(""){
          case (a, (num, list)) =>
            val str = list.foldLeft(""){
              case (aa, s) =>
                aa + s"#^ :in_paper ${toURI(item)}; :on_page ${num} ; :from_chunk ${s.fromChunk} ; :to_chunk ${s.toChunk} ; :from_token ${s.fromToken} ; :to_token ${s.toToken}\n"
            }
            a + str
        }
        acc + sstr
    }
    println("COMMENT CHANGED TO "+result)
    result
  }

  val hasComment = comment.map(c => c == "")

  override def newItemView(name: String, paper: Paper): PublicationView = this.constructItemView(name){
    case (el, params)=>
      el.id = name
      val v = new PublicationView(el, paperURI,  Var(paper), kappaCursor).withBinder(v=>new CodeBinder(v))
      v
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected).withBinder(new GeneralBinder(_)))
    //.register("Bookmarks")((el, args) => new BookmarksView(el, location, null).withBinder(new GeneralBinder(_)))

  override def updateView(view: PublicationView, key: String, old: Paper, current: Paper): Unit = {
    view.paper() = current
  }
}


