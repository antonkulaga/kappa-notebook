package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.Events
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.notebook.KappaHub
import org.querki.jquery.$
import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic
import rx._

import scala.annotation.tailrec


class PapersView(val elem: Element, selected: Var[String], hub: KappaHub) extends Annotator {


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
    /*lastSelections().nonEmpty || */currentSelection().nonEmpty
  }

  val comments = Rx{
  val opt = currentSelection.now.headOption//lastSelections.now.headOption
    val loc =
    "#^ :in_paper "+paper() +
      "\n#^ :on_page "+ page() + lastSelections().foldLeft(""){
      case (acc, el) => acc + "\n#^ :has_text " + el.text
    }
    println("loc = "+loc)
    loc
  }



  val nextPage = Var(Events.createMouseEvent())
  val previousPage = Var(Events.createMouseEvent())
  val addNugget= Var(Events.createMouseEvent())

  protected def insertInto(line: Int, code: String) = {

  }


  override def bindView(): Unit = {
    super.bindView()
    subscribePapers()
   }

  override def subscribePapers():Unit = {
    nextPage.triggerLater{
      val b = location.now
      println(s"next click ${b.page + 1}")
      location() = location.now.copy(page = b.page +1)
      //println("nextPageClick works")
      //paperManager.currentPaper.now.nextPage()
    }
    previousPage.triggerLater{
      val b = location.now
      println("previous click")
      //println("previousPageClick works")
      //paperManager.currentPaper.now.previousPage()
      location() = location.now.copy(page = b.page - 1)
    }
    addNugget.triggerLater{
      val position = hub.kappaCursor
      hub.kappaCode() = hub.kappaCode.now.withInsertion(position.now.line, comments.now)

    }
    super.subscribePapers()
    dom.window.document.onselectionchange = onSelectionChange _
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
  else if (node.isEqualNode(textLayerDiv) || textLayerDiv == node || textLayerDiv.isSameNode(node)) true
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