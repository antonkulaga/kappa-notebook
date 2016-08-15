package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.extensions._
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.notebook.extensions._
import org.denigma.kappa.notebook.parsers.PaperSelection
import org.denigma.pdf.extensions.Page
import org.querki.jquery.$
import org.scalajs.dom
import org.scalajs.dom.ext._
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Element, _}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.annotation.tailrec
import scala.collection.immutable._
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

class PublicationView(val elem: Element,
                      val selected: Var[String],
                      val paper: Var[Paper]
                     )
  extends PaperView {

  override type ItemView = ArticlePageView

  lazy val selections: Var[Set[PaperSelection]] = Var(Set.empty[PaperSelection])

  def scrollTo(selection: PaperSelection, retry: Int = 5, timeout: FiniteDuration = 800 millis): Unit = {
      itemViews.now.get(selection.page) match {
        case Some(v) =>
          v.getSpans(selection).collectFirst{case e: HTMLElement =>e} match {
            case Some(e) =>
              scalajs.js.timers.setTimeout(50 millis) {
                $(elem).dyn.scrollTo(e)
                v.highlight(selection)
                //val top = e.getBoundingClientRect().top - elem.getBoundingClientRect().top
                //println(s"offset ${e.offsetTop}")
                //elem.scrollT=op = top

              }
            case None =>
              if(paper.now.numPages >= selection.page && retry > 0){
                dom.console.log(s"page ${selection.page} is not yet loaded, retrying ...")
                scalajs.js.timers.setTimeout(timeout){scrollTo(selection, retry -1)}
              }
              else{
                dom.console.error(s"selection selects and mepty element, selection is ${selection.page}")
              }
          }

        case None =>
          if(paper.now.numPages >= selection.page && retry > 0){
            dom.console.log(s"page ${selection.page} is not yet loaded, retrying ...")
            scalajs.js.timers.setTimeout(timeout){scrollTo(selection, retry -1)}
          }
          else{
            dom.console.error(s"selection selects and mepty element, selection is ${selection.page}")
          }
      }
  }

  lazy val active: rx.Rx[Boolean] = selected.map{
    value =>
      //println(s"VALUE = ${value} PAPER = ${paper.now.name}")
      value == paper.now.name
  }

  override  val paperContainer = elem.children.collectFirst{
    case e: HTMLElement if e.classList.contains("paper-container")=> e
  }.getOrElse(elem)

  @tailrec final def nodeInside(node: Node, e: Element): Boolean = if(node == null) false
  else if (/*node.isEqualNode(textLayer) || */e == node || e.isSameNode(node)) true
  else if(node.parentNode == null) false else nodeInside(node.parentNode, e)

  def select(sel: Selection): Map[Int, List[TextLayerSelection]] = {
    val children = itemViews.now
    children.map { case (num, child) => (num, child.textLayerSelection(sel)) }
  }

  lazy val scale = Var(1.4)

  scale.onChange{ value => dom.console.log(s"scale changed to $value")}

  override def newItemView(item: Int, value: Page): ItemView = this.constructItemView(item){
    case (el, args) =>
      val view = new ItemView(el, item, value, scale).withBinder(v=>new CodeBinder(v))
      view
  }

  override def updateView(view: ArticlePageView, key: Int, old: Page, current: Page): Unit = {
    dom.console.error("page view should be not updateble!")
  }

  protected def seqRender(num: Int, p: Paper, retries: Int = 0): Unit = if(num < p.numPages){
    p.loadPage(num).onComplete{
      case Success(page) =>
        this.items() = items.now.updated(page.num, page)
        import scala.concurrent.duration._
        scalajs.js.timers.setTimeout(100 millis)(seqRender(num + 1, p, 0))

      case Failure(th) =>
        dom.console.error(s"cannot load page $num in paper ${p.name} with exception ${th}")
        if(retries < 3) seqRender(num, p, retries +1)
    }
  }

  override def subscribeUpdates() = {
    super.subscribeUpdates()
    paper.foreach{
      case EmptyPaper => //do nothing
      case p: PaperPDF => seqRender(1, p)
    }
    selections.updates.onChange{
      upd =>
        upd.removed.foreach{
          r => itemViews.now.get(r.page).foreach{ rv =>
            rv.unhighlight( r)
          }
        }
        upd.added.foreach {
          a => itemViews.now.get(a.page).foreach { av =>
            av.highlight( a)
          }

        }
    }
 }

}

class ArticlePageView(val elem: Element,
                      val num: Int,
                      val page: Page,
                      val scale: Rx[Double]
                     )  extends PageView {
 val name = Var("page_"+num)
 val title = Var("page_"+num)

  def textLayerSelection(sel: Selection): List[TextLayerSelection] = {
    if(sel.anchorNode.isInside(textDiv) || sel.focusNode.isInside(textDiv)) sel.map{ range =>
      TextLayerSelection.fromRange("", range)}.toList
    else List.empty[TextLayerSelection]
  }


  override lazy val canvas = {
    elem.getElementsByTagName("canvas").collectFirst{
      case canv: Canvas => canv
    }.orElse(elem.children.collectFirst {
        case canv: Canvas => canv
      }).get
  } //unsafe

  override lazy val textDiv = elem.getElementsByClassName("textLayer").collectFirst{
    case el: HTMLDivElement => el
  }.orElse(elem.children.collectFirst {
    case canv: HTMLDivElement => canv
  }).get //unsafe

  override def bindView() = {
    super.bindView()
  }

  def getSpans(sel: TextLayerSelection): List[Element] = sel.selectTokenSpans(textDiv)


  def unhighlight(sel: TextLayerSelection ) = {
    val spans = getSpans(sel)
    dom.console.log(s"DE_HIGHLIGHT fromChunk ${sel.fromChunk} toChunk ${sel.toChunk} fromToken ${sel.fromToken} toToken ${sel.toToken} spans length = ${spans.length}")
    spans.foreach { sp =>
      if (sp.classList.contains("highlight"))
        sp.classList.remove("highlight")
    }
  }


  def highlight(sel: TextLayerSelection) = {
    //println("chunks are: ")
    val spans = getSpans(sel)
    //spans.foreach(s=>println(s.outerHTML))
    dom.console.log(s"HIGHLIGHT fromChunk ${sel.fromChunk} toChunk ${sel.toChunk} fromToken ${sel.fromToken} toToken ${sel.toToken} spans length = ${spans.length}")
    spans.foreach{ sp=> if(!sp.classList.contains("highlight")) sp.classList.add("highlight") }
  }


}