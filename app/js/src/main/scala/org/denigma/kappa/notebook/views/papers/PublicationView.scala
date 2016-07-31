package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.notebook.views.common.TabItem
import org.denigma.pdf.extensions.Page
import org.scalajs.dom
import org.scalajs.dom.raw
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.scalajs.dom.ext._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

class PublicationView(val elem: Element,
                      val location: Var[Bookmark],
                      val paper: Var[Paper],
                      kappaCursor: Var[Option[(Editor, PositionLike)]]
                     )
  extends PaperView with TabItem{

  override  val paperContainer = elem.children.collectFirst{
    case e: HTMLElement if e.classList.contains("paper-container")=> e
  }.getOrElse(elem)


  lazy val scale = Var(1.4)

  scale.onChange{
    case value =>
      println(s"scale changed to $value")
  }

  val addNugget= Var(Events.createMouseEvent())

  val selected: Rx[String] = location.map(l=>l.paper)

  val selections  = Var(List.empty[TextLayerSelection])

  val paperURI = paper.map(p=>p.name)

  val selectionRanges: Var[List[raw.Range]] = Var(List.empty[org.scalajs.dom.raw.Range])

  val lastSelections: Var[List[TextLayerSelection]] = Var(List.empty[TextLayerSelection])

  val hasSelection = Rx{
    lastSelections().nonEmpty || selectionRanges().nonEmpty
  }

  override type ItemView = ArticlePageView

  override def newItemView(item: Int, value: Page): ItemView = this.constructItemView(item){
    case (el, args) =>
      val view = new ItemView(el, item, value, scale).withBinder(v=>new CodeBinder(v))
      view
  }

  override def updateView(view: ArticlePageView, key: Int, old: Page, current: Page): Unit = {
    dom.console.error("page view should be not updateble!")
  }

/*
  @tailrec final def inTextLayer(node: Node): Boolean = if(node == null) false
  else if (/*node.isEqualNode(textLayer) || */textLayerDiv == node || textLayerDiv.isSameNode(node)) true
  else if(node.parentNode == null) false else inTextLayer(node.parentNode)

  val ranges = Var(List.empty[Range])

  protected def onSelectionChange(event: Event) = {
    val selection: Selection = dom.window.getSelection()
    val count = selection.rangeCount
    inTextLayer(selection.anchorNode) || inTextLayer(selection.focusNode)  match {
      case true =>
        if (count > 0) {
          val values: List[Range] = {
            for{
              i <- 0 until count
              range = selection.getRangeAt(i)
            } yield range
          }.toList
          ranges() = values
          //selections() = values
          //val text = selections.foldLeft("")((acc, el)=>acc + "\n" + el.cloneContents().textContent)
          //currentSelection() = text
        }
      case false => //println(s"something else ${selection.anchorNode.textContent}") //do nothing
    }
  }
*/
  override def subscribeUpdates() = {
    super.subscribeUpdates()
    paper.foreach{
      case EmptyPaper => //do nothing
      case p: PaperPDF =>
        for(i <- 1 until p.numPages){
          p.loadPage(i).onComplete{
            case Success(page) => this.items() = items.now.updated(page.num, page)
            case Failure(th) => dom.console.error(s"cannot load page $i in paper ${p.name} with exception ${th}")
          }
        }
    }
    addNugget.triggerLater{
      kappaCursor.now match
      {
        case Some((ed, position)) =>
        //hub.kappaCode() = hub.kappaCode.now.withInsertion(position.line, comments.now)
        case None =>
      }
    }
    /*
   dom.document.addEventListener("selectionchange", onSelectionChange _)
   //textLayerDiv.parentNode.addEventListener(Events.mouseleave, fixSelection _)
   ges.onChange{
     case value =>
       println("RANGES CHANGE DETECTED!")
       println(value.mkString("\n"))
   }*/
 }

}

class ArticlePageView(val elem: Element, val num: Int, val page: Page, val scale: Rx[Double])  extends PageView {
 val name = Var("page_"+num)
 val title = Var("page_"+num)
}

/*
class PublicationView(val elem: Element,
                     val location: Var[Bookmark],
                     val paper: Paper,
                     kappaCursor: Var[Option[(Editor, PositionLike)]]
                    )
 extends PaperView with TabItem
{

 scale.onChange{
   case value =>
     println(s"scale changed to $value")
 }

 val selected: Rx[String] = location.map(l=>l.paper)

 val selections  = Var(List.empty[TextLayerSelection])

 val paperURI = Var(paper.name)

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
   dom.document.addEventListener("selectionchange", onSelectionChange _)
   //textLayerDiv.parentNode.addEventListener(Events.mouseleave, fixSelection _)
   ranges.onChange{
     case value =>
       println("RANGES CHANGE DETECTED!")
       println(value.mkString("\n"))
   }
 }

 @tailrec final def inTextLayer(node: Node): Boolean = if(node == null) false
 else if (/*node.isEqualNode(textLayer) || */textLayerDiv == node || textLayerDiv.isSameNode(node)) true
 else if(node.parentNode == null) false else inTextLayer(node.parentNode)

 val ranges = Var(List.empty[Range])

 protected def onSelectionChange(event: Event) = {
   val selection: Selection = dom.window.getSelection()
   val count = selection.rangeCount
   inTextLayer(selection.anchorNode) || inTextLayer(selection.focusNode)  match {
     case true =>
       if (count > 0) {
         val values: List[Range] = {
           for{
             i <- 0 until count
             range = selection.getRangeAt(i)
           } yield range
         }.toList
         ranges() = values
         //selections() = values
         //val text = selections.foldLeft("")((acc, el)=>acc + "\n" + el.cloneContents().textContent)
         //currentSelection() = text
       }
     case false => //println(s"something else ${selection.anchorNode.textContent}") //do nothing
   }
 }



 override def bindView() = {
   super.bindView()
   subscribePapers()
 }
}
/*
trait LoadedPaperView extends BindableView {

 lazy val page: Var[Int] = Var(1)

 lazy val scale = Var(1.5)

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
   case Some(pg) =>
     deselect(textLayerDiv)
     val pageRenderer = new PageRenderer(pg)
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
*/
*/