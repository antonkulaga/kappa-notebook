package org.denigma.kappa.notebook.views.annotations.papers

import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, UpdatableView}
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.papers._
import org.denigma.controls.pdf.{PDFPageProxy, PDFPageViewport, TextLayerBuilder, TextLayerOptions}
import org.denigma.kappa.notebook.WebSocketTransport
import org.denigma.kappa.notebook.views.common.TabItem
import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Element, _}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.List
import scala.annotation.tailrec
import scala.collection.immutable.{Map, _}
import scala.scalajs.js
import scala.util.{Failure, Success}
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.denigma.controls.pdf.extensions._

class PublicationView(val elem: Element,
                      val selected: Var[String],
                      val paper: Paper,
                      kappaCursor: Var[Option[(Editor, PositionLike)]]
                     )
  extends  LoadedPaperView with TabItem with UpdatableView[Paper]
{

  val paperURI = Var(paper.name)

  val canvas: Canvas  = elem.getElementsByClassName("canvas")(0).asInstanceOf[Canvas]

  val textLayerDiv: Element = elem.getElementsByClassName("textLayer")(0).asInstanceOf[HTMLElement]//dom.document.getElementById(textLayer)//.asInstanceOf[HTMLElement]

  val currentPageNum: Rx[Int] = currentPage.map{
    case None=> 0
    case Some(num) => num.num
  }


  val selections = Var(List.empty[org.scalajs.dom.raw.Range])

  val currentSelection = selections.map{ case sel =>
    sel.foldLeft("")((acc, el)=>acc + "\n" + el.cloneContents().textContent)
  }

  val lastSelections: Var[List[TextSelection]] = Var(List.empty[TextSelection])

  val hasSelection: Dynamic[Boolean] = Rx{
    lastSelections().nonEmpty || currentSelection().nonEmpty
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

  lazy val scale = Var(1.4)

  def paper: Paper

  def canvas: Canvas

  val textLayerDiv: Element

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


  protected def onPageChange(pageOpt: Option[Page]): Unit =  pageOpt match
  {
    case Some(pg) =>
      println("PAGE CHANGED!!!")
      //println(s"page option change with ${page}")
      val viewport: PDFPageViewport = pg.viewport(scale.now)
      var context = canvas.getContext("2d")//("webgl")
      canvas.height = viewport.height.toInt
      canvas.width =  viewport.width.toInt
      pg.render(js.Dynamic.literal(
        canvasContext = context,
        viewport = viewport
      ))
      val textContentFut = pg.textContentFut.onComplete{
        case Success(textContent) =>
          println("TEXT CONTENT")
          alignTextLayer(viewport)
          textLayerDiv.innerHTML = ""
          val textLayerOptions = new TextLayerOptions(textLayerDiv, 1, viewport)
          val textLayer = new TextLayerBuilder(textLayerOptions)
          textLayer.setTextContent(textContent)
          val p = new TextBuilder(textLayerDiv, 1, viewport)
          //println(textContent+"!!! is TEXT")
          textLayer.render()
          //updateSelection(textLayerDiv)

        case Failure(th) =>
          //dom.console.error(s"cannot load the text layer for ${location.now}")
      }

    case None =>
      println("CANNOT FIND A CHANGE")
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
/*
class PublicationView(val elem: Element,
                      val currentProjectName: Rx[String],
                      val subscriber: WebSocketTransport,
                      val selected: Var[String],
                      val location: Var[Bookmark],
                      kappaCursor: Var[Option[(Editor, PositionLike)]]
                     )
  extends Annotator with UpdatableView[Bookmark] with TabItem
{

  lazy val loadedPapers = Var(Map.empty[String, Paper])

  lazy val paperLoader: PaperLoader = WebSocketPaperLoader(subscriber, currentProjectName, loadedPapers)

  scale.Internal.value = 1.4

  location.triggerLater(
    selected() = this.id
  )

  val paperURI = location.map(_.paper)

  val canvas: Canvas  = elem.getElementsByClassName("canvas")(0).asInstanceOf[Canvas]

  val textLayerDiv: Element = elem.getElementsByClassName("textLayer")(0).asInstanceOf[HTMLElement]//dom.document.getElementById(textLayer)//.asInstanceOf[HTMLElement]


  //val paperName = paperManager.currentPaper.map(_.name)

  val paper = location.map(_.paper)

  val page = location.map(_.page)

  val selections = Var(List.empty[org.scalajs.dom.raw.Range])

  val currentSelection = selections.map{ case sel =>
    sel.foldLeft("")((acc, el)=>acc + "\n" + el.cloneContents().textContent)
  }

  val lastSelections: Var[List[TextSelection]] = Var(List.empty[TextSelection])

  val hasSelection: Dynamic[Boolean] = Rx{
    lastSelections().nonEmpty || currentSelection().nonEmpty
  }

  val comments = Rx{
    //val opt = currentSelection.now.headOption//lastSelections.now.headOption
    val loc =
      "#^ :in_paper "+paper() +
        "\n#^ :on_page "+ page() + lastSelections().foldLeft(""){
        case (acc, el) => acc + "\n#^ :has_text " + el.text
      }
    //println("loc = "+loc)
    loc
  }



  val nextPage = Var(Events.createMouseEvent())
  val previousPage = Var(Events.createMouseEvent())
  val addNugget= Var(Events.createMouseEvent())
  /*
  TODO: fix it!
  does not work in firefox!!!
  val scroll= Var(Events.createWheelEvent())
  scroll.onChange{
    case wheel=>
      println("DELTAY = "+ wheel.deltaY)
      scale() = scale.now + wheel.deltaY / 1000
  }
  elem.addEventListener[WheelEvent]("onwheel", {event: WheelEvent => scroll() = event })
  elem.addEventListener[WheelEvent]("onmousewheel", {event: WheelEvent => scroll() = event })

  */
  protected def insertInto(line: Int, code: String) = {

  }

  override def bindView(): Unit = {
    super.bindView()
    subscribePapers()
  }

  override def subscribePapers(): Unit = {
    nextPage.triggerLater{
      val b = location.now
      location() = location.now.copy(page = b.page +1)
    }
    previousPage.triggerLater{
      val b = location.now
      location() = location.now.copy(page = b.page - 1)
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
    dom.document.addEventListener("selectionchange", onSelectionChange _)
    textLayerDiv.parentNode.addEventListener(Events.mouseleave, fixSelection _)
  }

  protected def rangeToTextSelection(range: org.scalajs.dom.raw.Range) = {
    val fragment = range.cloneContents()
    /*
    val div = dom.document.createElement("div") //the trick to get inner html of the selection
    val nodes = fragment.childNodes.toList
    nodes.foreach(div.appendChild)
    val txt = div.innerHTML
    */
    val txt = fragment.textContent
    TextSelection(txt)
  }

  protected def fixSelection(event: Event): Unit = {
    //println("mouseleave")
    if(currentSelection.now != "") {
      val ss = selections.now.map(rangeToTextSelection)
      //println(ss)
      lastSelections() = ss
      selections() = List.empty
      //currentSelection() = ""
    }
  }

  @tailrec final def inTextLayer(node: Node): Boolean = if(node == null) false
  else if (node.isEqualNode(textLayerDiv) || textLayerDiv == node || textLayerDiv == node) true
  else if(node.parentNode == null) false else inTextLayer(node.parentNode)


  protected def onSelectionChange(event: Event) = {
    //println("selection change works!")
    val selection: Selection = dom.window.getSelection()
    val count = selection.rangeCount
    inTextLayer(selection.anchorNode) || inTextLayer(selection.focusNode)  match {
      case true =>
        if (count > 0) {
          selections() = {
            for{
              i <- 0 until count
              range = selection.getRangeAt(i)
            } yield range
          }.toList
          //val text = selections.foldLeft("")((acc, el)=>acc + "\n" + el.cloneContents().textContent)
          //currentSelection() = text
        }
      case false => //println(s"something else ${selection.anchorNode.textContent}") //do nothing
    }

  }

  override def update(value: Bookmark): PublicationView.this.type = {
    location() = value
    this
  }
}
*/