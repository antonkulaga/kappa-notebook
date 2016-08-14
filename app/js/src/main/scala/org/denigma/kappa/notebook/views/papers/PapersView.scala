package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionMapView}
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.messages.{GoToPaper, GoToPaperSelection, KappaBinaryFile}
import org.denigma.kappa.notebook._
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.views.common.TabHeaders
import org.scalajs.dom
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.parsers.PaperSelection
import org.denigma.kappa.notebook.views.actions.Movements
import org.denigma.kappa.notebook.views.editor.{EmptyCursor, KappaCursor, KappaEditorCursor}

import scala.collection.immutable._
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.{Failure, Success}

class PapersView(val elem: Element,
                 val connector: WebSocketTransport,
                 val projectPapers: Rx[Map[String, KappaBinaryFile]],
                 val kappaCursor: Var[KappaCursor]) extends BindableView
  with CollectionMapView {

  val paperSelections: Var[List[PaperSelection]] = Var(Nil)
  val currentSelection = paperSelections.map(s => s.headOption)

  val paperURI: Var[String] = Var("")

  val paperLoader: WebSocketPaperLoader = WebSocketPaperLoader(connector, projectPapers)

  val items: Var[Map[String, Paper]] = paperLoader.loadedPapers

  val empty = items.map(its=>its.isEmpty)

  override type Key = String

  override type Value = Paper

  override type ItemView = PublicationView

  lazy val line: Rx[String] = kappaCursor.map{
    case KappaEditorCursor(file, ed, l, ch) => l +":" + ch
    case _ => ""
  }

  lazy val codeFile = kappaCursor.map{
    case KappaEditorCursor(file, ed, l, ch) =>  file.path
    case _ => ""
  }

  val headers = itemViews.map(its=> SortedSet.empty[String] ++ its.values.map(_.id))

  //lazy val selections = itemViews.map(its => its.sele)

  paperURI.foreach{
    case paper if paper!="" =>
      //println("LOAD :"+loc.paper)
      paperLoader.getPaper(paper, 10 seconds).onComplete{
        case Success(pp) =>
          paperLoader.loadedPapers() = paperLoader.loadedPapers.now.updated(pp.name, pp)

          //selected() = pp.name
        case Failure(th)=> dom.console.error(s"Cannot load paper ${paper}: "+th)
      }
    case _ => //do nothing
  }

  val toSource = Var(Events.createMouseEvent())
  toSource.onChange{
    ev => currentSelection.now match {
      case Some(s) => dom.console.log(s"click on $s")//input() = Movements.toFile(s.)
      case None =>
    }
  }

  val selectionOpt: Var[Option[Selection]] = Var(None)

  connector.input.onChange {
    case GoToPaper(loc)=> paperURI() = loc
    case other => //do nothing
  }

  lazy val lineNumber = kappaCursor.map{c => c.lineNum}
  lazy val charNumber = kappaCursor.map{c => c.ch}

  val canInsert: Rx[Boolean] = kappaCursor.map(c => c != EmptyCursor)
  val insertComment = Var(Events.createMouseEvent())
  insertComment.triggerLater{
    kappaCursor.now match {
      case c @ KappaEditorCursor(file, ed, lineNum, ch) =>
        val doc = ed.getDoc()
        val line = doc.getLine(lineNum)
        doc.replaceRange(comment.now, c.position, c.position)
        //ed.getDoc().setLine(pos.line, line + comment.now)
        //dom.console.log(s"POSITION = ${pos.line} ch is ${pos.ch} LEN IS ${line.length}")
      case _ => dom.console.error("EDITOR IS NOT AVALIABLE TO INSERT")
    }
  }

  override protected def subscribeUpdates(): Unit =
  {
    super.subscribeUpdates()
    selectionOpt.afterLastChange(900 millis){
      case Some(sel) =>
        val sels: Map[Item, Map[Int, List[TextLayerSelection]]] =  itemViews.now.map{ case (item, child) => (item, child.select(sel)) }
        selections() = sels

      case None =>
        //println("selection option is NONE")
        selections() = Map.empty[Item, (Map[Int, List[TextLayerSelection]])]

    }
    dom.document.addEventListener("selectionchange", onSelectionChange _)
  }

  protected def onSelectionChange(event: Event) = {
    val selection: Selection = dom.window.getSelection()
    selection.anchorNode.isInside(elem) || selection.focusNode.isInside(elem)  match {
      case true =>
        selectionOpt.Internal.value = Some(selection)
        selectionOpt.recalc()
      case false =>
    }
  }

  val selections: Var[Map[Item, (Map[Int, List[TextLayerSelection]])]] = Var(Map.empty[Item, (Map[Int, List[TextLayerSelection]])])

  protected def toURI(str: String) = str.replace(" ", "%20") match {
    case s if s.contains(":") => s
    case other => ":" + other
  }

  val comment: Rx[String] = selections.map{ chosen =>
    val result = chosen.foldLeft(""){
      case (acc, (item, mp))=>
        val sstr = mp.foldLeft(""){
          case (a, (num, list)) =>
            val str = list.foldLeft(""){
              case (aa, s) =>
                aa + s"#^ :in_paper ${toURI(item)}; :on_page ${num}; :from_chunk ${s.fromChunk}; :to_chunk ${s.toChunk}; :from_token ${s.fromToken}; :to_token ${s.toToken} .\n"
            }
            a + str
        }
        acc + sstr
    }
    result
  }

  val hasComment = comment.map(c => c != "")

  override def newItemView(item: Key, paper: Paper): PublicationView = this.constructItemView(item){
    case (el, params)=>
      el.id = paper.name
      val v = new PublicationView(el, paperURI,  Var(paper), connector.input).withBinder(v=>new CodeBinder(v))
      v
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, paperURI)(TabHeaders.path2name).withBinder(new GeneralBinder(_)))
    //.register("Bookmarks")((el, args) => new BookmarksView(el, location, null).withBinder(new GeneralBinder(_)))

  override def updateView(view: PublicationView, key: Key, old: Paper, current: Paper): Unit = {
    view.paper() = current
  }
}


