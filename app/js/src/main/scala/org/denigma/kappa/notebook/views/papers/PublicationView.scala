package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.{GeneralBinder, Events}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{UpdatableView, BindableView}
import org.denigma.codemirror.{PositionLike, Editor}
import org.denigma.controls.papers.{Bookmark, TextLayerSelection, Paper}
import org.denigma.kappa.notebook.views.common.TabItem
import org.denigma.pdf.PDFPageViewport
import org.denigma.pdf.extensions.{Page, PageRenderer, TextLayerRenderer}
import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw
import org.scalajs.dom.raw.{HTMLElement, Element}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.{Failure, Success}

class PublicationView(val elem: Element,
                      val location: Var[Bookmark],
                      val paper: Paper,
                      kappaCursor: Var[Option[(Editor, PositionLike)]]
                     )
  extends  LoadedPaperView with TabItem with UpdatableView[Paper]
{

  scale.onChange{
    case value =>
      println(s"scale changed to $value")
  }

  val selected: Rx[String] = location.map(l=>l.paper)

  val selections  = Var(List.empty[TextLayerSelection])

  val paperURI = Var(paper.name)

  val canvas: Canvas  = elem.getElementsByClassName("canvas")(0).asInstanceOf[Canvas]

  val textLayerDiv = elem.getElementsByClassName("textLayer")(0).asInstanceOf[HTMLElement].asInstanceOf[HTMLElement]

  val currentPageNum: Rx[Int] = currentPage.map{
    case None=> 0
    case Some(num) => num.num
  }

  val selectionRanges: Var[List[raw.Range]] = Var(List.empty[org.scalajs.dom.raw.Range])

  val lastSelections: Var[List[TextLayerSelection]] = Var(List.empty[TextLayerSelection])

  val hasSelection = Rx{
    lastSelections().nonEmpty || selectionRanges().nonEmpty
  }

  override def subscribePapers(): Unit = {
    nextPage.triggerLater{
      println("NEXT PAGE WORKS")
      val pg = page.now
      page() = pg + 1
    }
    previousPage.triggerLater{
      val pg = page.now
      page() = pg - 1
    }
    addNugget.triggerLater{
      kappaCursor.now match
      {
        case Some((ed, position)) =>
        //hub.kappaCode() = hub.kappaCode.now.withInsertion(position.line, comments.now)
        case None =>
      }
    }

    scale.onChange{
      case sc=> refreshPage()
    }

    super.subscribePapers()
    //dom.document.addEventListener("selectionchange", onSelectionChange _)
    //textLayerDiv.parentNode.addEventListener(Events.mouseleave, fixSelection _)
  }


  override def bindView() = {
    println("bind view")
    super.bindView()
    subscribePapers()
  }

  override def update(value: Paper): PublicationView.this.type = {
    println("update is not implemented")
    this
  }
}

trait LoadedPaperView extends BindableView {

  lazy val page: Var[Int] = Var(1)

  lazy val scale = Var(1.6)

  def paper: Paper

  def canvas: Canvas

  val textLayerDiv: HTMLElement

  val selections: Rx[scala.List[TextLayerSelection]]

  val currentPage: Var[Option[Page]] = Var(None)

  val hasNextPage: Rx[Boolean] = Rx{
    val page = currentPage()
    page.isDefined && paper.hasPage(page.get.num + 1)
  }

  val hasPreviousPage: Rx[Boolean] = Rx{
    val page = currentPage()
    page.isDefined && paper.hasPage(page.get.num - 1)
  }

  val nextPage = Var(Events.createMouseEvent())
  val previousPage = Var(Events.createMouseEvent())
  val addNugget= Var(Events.createMouseEvent())

  protected def deselect(element: Element) = {
    selections.now.foreach {
      case sel =>
        val spans = sel.selectTokenSpans(element)
        spans.foreach {
          case sp =>
            if (sp.classList.contains("highlight"))
              sp.classList.remove("highlight")
        }
    }
  }

  protected def select(element: Element) = {
    selections.now.foreach{
      case sel=>
        println("chunks are: ")
        val spans = sel.selectTokenSpans(element)
        //spans.foreach(s=>println(s.outerHTML))
        spans.foreach{
          case sp=>
            if(!sp.classList.contains("highlight"))
              sp.classList.add("highlight")
        }
    }
  }


  protected def onPageChange(pageOpt: Option[Page]): Unit =  pageOpt match
  {
    case Some(page) =>
      deselect(textLayerDiv)
      val pageRenderer = new PageRenderer(page)
      pageRenderer.render(canvas, textLayerDiv, scale.now).onComplete{
        case Success(result)=>
          select(textLayerDiv)
        case Failure(th) =>
         // dom.console.error(s"cannot load the text layer for ${location.now}")
      }
    case None =>
      //println("nothing changes")
      textLayerDiv.innerHTML = ""
  }

  def refreshPage() = {
    if(currentPage.now.isDefined) {
     loadPage(currentPage.now.get.num)
    } else {
      "the paper is empty!"
    }

  }

  protected def loadPage(num: Int) = {
    paper.getPage(num).onSuccess{
      case pg: Page =>
        currentPage() = Some(pg)//onPageChange(Some(pg))
    }
  }


  protected def subscribePapers(): Unit ={
    page.onChange(p=>loadPage(p))
    currentPage.onChange(onPageChange)
    //location.foreach(onLocationUpdate)
    scale.onChange{ case sc=> refreshPage()  }
    if(paper.numPages > 0) loadPage(1)
  }

}