package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.Events
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.controls.pdf.{PDFPageViewport, TextLayerBuilder, TextLayerOptions}
import org.denigma.kappa.notebook.KappaHub
import org.querki.jquery.$
import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._
import org.denigma.binding.extensions._

import scala.annotation.tailrec
import scala.scalajs.js
import scala.util.{Failure, Success}
import scalajs.concurrent.JSExecutionContext.Implicits.queue


class PapersView(val elem: Element, selected: Var[String], hub: KappaHub) extends Annotator {


  val scale = Var(1.4)

  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)

  //start location to run
  val location = hub.paperLocation

  location.triggerLater(
    selected() = this.id
  )

  val paperURI = location.map(_.paper)


  val canvas: Canvas  = $("#the-canvas").get(0).asInstanceOf[Canvas]
  //val $textLayerDiv: JQuery = $("#text-layer")
  val textLayerDiv: Element = dom.document.getElementById("text-layer")//.asInstanceOf[HTMLElement]

  //val paperName = paperManager.currentPaper.map(_.name)

  val paper = location.map(_.paper)
  val page = location.map(_.page)

  val selections = Var(List.empty[Range])

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
  val scroll= Var(Events.createWheelEvent())

  protected def insertInto(line: Int, code: String) = {

  }

  /**
    * Refreshes page
    */
  def refreshesPage() = if(this.currentPage.now.nonEmpty){
    val paper = currentPaper.now
    paper.getPage(currentPage.now.get.num).onSuccess{
      case pg: Page => onPageChange(Some(pg))
    }
  }

  override protected def onPageChange(pageOpt: Option[Page]): Unit =  pageOpt match
  {
    case Some(page) =>
      //println(s"page option change with ${page}")
      //var scale = 1.0
      val viewport: PDFPageViewport = page.viewport(scale.now)
      var context = canvas.getContext("2d")//("webgl")
      canvas.height = viewport.height.toInt
      canvas.width =  viewport.width.toInt
      page.render(js.Dynamic.literal(
        canvasContext = context,
        viewport = viewport
      ))
      val textContentFut = page.textContentFut.onComplete{
        case Success(textContent) =>
          alignTextLayer(viewport)
          textLayerDiv.innerHTML = ""
          val textLayerOptions = new TextLayerOptions(textLayerDiv, 1, viewport)
          val textLayer = new TextLayerBuilder(textLayerOptions)
          textLayer.setTextContent(textContent)
          //println(textContent+"!!! is TEXT")
          textLayer.render()
          updateSelection(textLayerDiv)

        case Failure(th) =>
          dom.console.error(s"cannot load the text layer for ${location.now}")
      }
    case None =>
      //println("nothing changes")
      textLayerDiv.innerHTML = ""
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
      val position = hub.kappaCursor
      hub.kappaCode() = hub.kappaCode.now.withInsertion(position.now.line, comments.now)
    }
    scroll.onChange{
      case wheel=>
        println("DELTAY = "+ wheel.deltaY)
        scale() = scale.now + wheel.deltaY / 1000
    }
    scale.onChange{
      case sc=>
        refreshesPage()
    }
    elem.addEventListener[WheelEvent]("onwheel", {event: WheelEvent => scroll() = event })
    elem.addEventListener[WheelEvent]("onmousewheel", {event: WheelEvent => scroll() = event })
    super.subscribePapers()
    dom.document.addEventListener("selectionchange", onSelectionChange _)
    textLayerDiv.parentNode.addEventListener(Events.mouseleave, fixSelection _)
  }

  protected def rangeToTextSelection(range: Range) = {
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


  /**
    * Register views
    */
  override lazy val injector = defaultInjector
    /*
    .register("Bookmarks"){
      case (el, args) =>  new BookmarksView(el, location, textLayerDiv).withBinder(new CodeBinder(_))
    }
  */

 }