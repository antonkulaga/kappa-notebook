package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionMapView}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.messages.{GoToPaper, GoToPaperSelection, KappaBinaryFile, KappaMessage}
import org.denigma.kappa.notebook._
import org.denigma.kappa.notebook.actions.{Commands, Movements}
import org.denigma.kappa.notebook.parsers.{AST, PaperSelection}
import org.denigma.kappa.notebook.views.annotations.CommentInserter
import org.denigma.kappa.notebook.views.common.{FileTabHeaders, FileTabItemView, TabHeaders}
import org.denigma.kappa.notebook.views.editor.KappaCursor
import org.scalajs.dom
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/**
  * This class manages papers
  * @param elem Element to which view is bound to
  * @param connector class that provides websocket connection and input/output vars to subscribe to
  * @param projectPapers papers of the project
  * @param kappaCursor is used to suggest insertions of comments
  */
class PapersView(val elem: Element,
                 val connector: WebSocketTransport,
                 val projectPapers: Rx[Map[String, KappaBinaryFile]],
                 val kappaCursor: Var[KappaCursor],
                 val movements: Movements
                ) extends BindableView
  with CollectionMapView with CommentInserter {

  val paperSelections: Var[List[PaperSelection]] = Var(Nil)

  val currentSelection = paperSelections.map(s => s.headOption)

  val paperURI: Var[String] = Var("")

  val paperLoader: WebSocketPaperLoader = WebSocketPaperLoader(connector, projectPapers)

  val items: Var[Map[String, Paper]] = paperLoader.loadedPapers

  val empty = items.map(its=>its.isEmpty)

  override type Key = String

  override type Value = Paper

  override type ItemView = PublicationView

  val headers = itemViews.map(its=> SortedSet.empty[String] ++ its.values.map(_.id))

  paperURI.foreach{
    case paper if paper!="" =>
      //println("LOAD :"+loc.paper)
      paperLoader.getPaper(paper, 10 seconds).onComplete{
        case Success(pp) => paperLoader.loadedPapers() = paperLoader.loadedPapers.now.updated(pp.name, pp)
        case Failure(th)=> dom.console.error(s"Cannot load paper ${paper}: "+th)
      }
    case _ => //do nothing
  }


  val toSource = Var(Events.createMouseEvent())
  toSource.onChange{
    ev => currentSelection.now match {
      case Some(s) => input() = movements.toSource(AST.IRI(codeFile.now), lineNumber.now, lineNumber.now)
      case None =>
    }
  }

  val selectionOpt: Var[Option[Selection]] = Var(None)

  lazy val input = connector.input

  //subscribption on some events/commands
  input.onChange {


    case GoToPaper(loc)=> paperURI() = loc //just switches to another paper

    case GoToPaperSelection(selection, exc) =>

      paperURI.Internal.value = selection.label //TODO: fix this ugly workaround

      paperLoader.getPaper(selection.label, 12 seconds).onComplete{
        case Success(paper) =>
          paperLoader.loadedPapers() = paperLoader.loadedPapers.now.updated(paper.name, paper)
          itemViews.now.values.foreach(p => p.selections() = Set.empty) //TODO: fix this ugly cleaning workaround
          itemViews.now.get(paperURI.now) match {
            case Some(v) =>
              v.selections() = if(exc) Set(selection) else v.selections.now + selection
              additionalComment() = ""
              v.scrollTo(selection, 5, 300 millis) //scrolls to loaded paper

            case None =>  dom.console.error(s"Paper URI for ${selection.label} does not exist")

          }
        case Failure(th)=> dom.console.error(s"Cannot load paper ${selection.label}: "+th)
      }

    case  Commands.CloseFile(path) =>
      if(items.now.contains(path)) {
      items() = items.now - path
    } else {
      //dom.console.error(s"cannot find ${path}")
    }

    case other => //do nothing

  }

  lazy val domSelections: Var[Map[Item, (Map[Int, List[TextLayerSelection]])]] = Var(Map.empty[Item, (Map[Int, List[TextLayerSelection]])])

  override protected def subscribeUpdates(): Unit =
  {
    super.subscribeUpdates()
    selectionOpt.afterLastChange(900 millis){
      case Some(sel) =>
        val sels: Map[Item, Map[Int, List[TextLayerSelection]]] =  itemViews.now.map{ case (item, child) => (item, child.select(sel)) }
        domSelections() = sels

      case None =>

    }
    dom.document.addEventListener("selectionchange", onSelectionChange _)
  }

  protected def insideTextLayer(selection: Selection) =
    selection.anchorNode.insidePartial{ case el: HTMLElement if el.classList.contains("textLayer") || el.classList.contains("textlayer")=>} ||
    selection.focusNode.insidePartial{ case el: HTMLElement if el.classList.contains("textLayer") || el.classList.contains("textlayer")=>}

  protected def onSelectionChange(event: Event) = {
    val selection: Selection = dom.window.getSelection()
    if (insideTextLayer(selection)) {
      selectionOpt.Internal.value = Some(selection)
      selectionOpt.recalc()
    }
  }

  protected def makeComment(chosen: Map[Item, (Map[Int, List[TextLayerSelection]])], addComment: String) = {
    val com = if(addComment=="") " " else ":comment \""+ addComment +"\"; "
    chosen.foldLeft(""){
      case (acc, (item, mp))=>
        val sstr = mp.foldLeft(""){
          case (a, (num, list)) =>
            val str = list.foldLeft(""){
              case (aa, s) =>
                aa + s"#^${com}:in_paper ${toURI(item)}; :on_page ${num}; :from_chunk ${s.fromChunk}; :to_chunk ${s.toChunk}; :from_token ${s.fromToken}; :to_token ${s.toToken} .\n"
            }
            a + str
        }
        acc + sstr
    }
  }

  override lazy val comment: Rx[String] = Rx{ //TODO: fix this bad unoptimized code
    val chosen = domSelections()
    val com = additionalComment()
    makeComment(chosen, com)
  }

  lazy val hasComment: Rx[Boolean] = comment.map(c => c != "")

  override def newItemView(item: Key, paper: Paper): PublicationView = this.constructItemView(item){
    case (el, params)=>
      el.id = paper.name
      val v = new PublicationView(el, paperURI,  Var(paper)).withBinder(v=>new CodeBinder(v))
      v
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new FileTabHeaders(el, headers, input, paperURI)(FileTabHeaders.path2name).withBinder(new GeneralBinder(_)))

  override def updateView(view: PublicationView, key: Key, old: Paper, current: Paper): Unit = {
    view.paper() = current
  }
}