package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.extensions._
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.notebook.parsers.PaperSelection
import org.denigma.malihu.scrollbar.JQueryScrollbar._
import org.denigma.malihu.scrollbar._
import org.denigma.pdf.extensions.Page
import org.querki.jquery.$
import org.scalajs.dom
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Element, _}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.annotation.tailrec
import scala.collection.immutable._
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.{Failure, Success}

/**
  * Class to show the paper, its subviews are pages
  * @param elem elements the view is bound to
  * @param selected Var shared between all papers
  * @param paper the paper
  */
class PublicationView(val elem: Element,
                      val selected: Var[String],
                      val paper: Var[Paper]
                     )
  extends PaperView {

  override type ItemView = ArticlePageView

  lazy val selections: Var[Set[PaperSelection]] = Var(Set.empty[PaperSelection])

  lazy val selectedPages = selections.map(s=>s.map(ss=>ss.page))

  lazy val scroller = initScroller()

  override def bindView(): Unit = {
    super.bindView()
    val sc = scroller //to init lazy value
  }

  /**
    * Initializes malihu custom scrollbar for the paper
    * @return
    */
  protected def initScroller(): JQueryScrollbar = {
    //val params = new mCustomScrollbarParams(theme = "rounded-dots-dark", axis = "y", advanced = new mCustomScrollbarAdvancedParams(true), mouseWheel = new MouseWheel(true))
    val callbacks = ScrollbarCallbacks.setWhileScrolling(updatePages).setOnScroll(updatePages)
    val params = mCustomScrollbarParams
      .theme("rounded-dots-dark")
      .axis("y")
      .advanced(new mCustomScrollbarAdvancedParams(true))
      .mouseWheel(new MouseWheel(true, disableOver = js.Array("select","option","keygen","datalist")))
      .callbacks(callbacks)
    $(elem).mCustomScrollbar(params)
  }

  /**
    * Is used p
    *
    */
  protected def updatePages() = {
    val pgs = selectedPages.now
    for((item, view) <- itemViews.now){
      if(view.checkVisibility() || pgs.contains(item)) view.makeVisible() else view.hide()
    }
  }

  /**
    * Scroll to the specific place in paper
    * @param selection
    * @param retry
    * @param timeout
    */
  def scrollTo(selection: PaperSelection, retry: Int = 5, timeout: FiniteDuration = 800 millis): Unit = {
      itemViews.now.get(selection.page) match {
        case Some(v) =>
          v.renderedPage.onComplete{
            case Success(p) =>
              v.getSpans(selection).collectFirst{case e: HTMLElement =>e} match {
                case Some(e) =>
                  updatePages()
                  scalajs.js.timers.setTimeout(200 millis) {
                    scroller.scrollTo(e)
                  }
                case None =>
                  if(paper.now.numPages >= selection.page && retry > 0){
                    dom.console.log(s"page ${selection.page} is not yet loaded, retrying ...")
                    scalajs.js.timers.setTimeout(timeout){scrollTo(selection, retry -1)}
                  }
                  else dom.console.error(s"selection selects and mepty element, selection is ${selection.page}")
              }
            case Failure(th) => dom.console.error(s"page ${selection.page} failed to render")
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


  /**
    * if the tab with publications is selected (active)
    */
  lazy val active: rx.Rx[Boolean] = selected.map{
    value => value == paper.now.name
  }

  /*
  HTML element where all pages of the paper will be loaded to
   */
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
    case (el: HTMLElement, args) =>
      val view = new ItemView(el, item, value, scale).withBinder(v=>new CodeBinder(v))
      view
    case (el, _) => throw new Exception(s"NOT AN HTMLElement: ${el.outerHTML}")
  }

  override def updateView(view: ArticlePageView, key: Int, old: Page, current: Page): Unit = {
    dom.console.error("page view should be not updateble!")
  }


  /**
    * Reqursive function that renders pages of the Paper
    * @param paper paper to render
    * @param num number of the page to render
    * @param retries how many retries to do in case of failure
    */
  protected def seqRender(paper: Paper, num: Int, retries: Int = 0): Unit = if(num <= paper.numPages){
    paper.loadPage(num).onComplete{
      case Success(page) =>
        this.items() = items.now.updated(page.num, page)
        import scala.concurrent.duration._
        scalajs.js.timers.setTimeout(300 millis){
          this.itemViews.now.get(page.num) match {
            case Some(v) =>
              v.renderedPage.onComplete{
                case Success(result) =>
                  seqRender(paper, num + 1, 0)
                case Failure(th) => dom.console.error(s"failed to load ${num} with error: $th")
                  seqRender(paper, num + 1, 0)
              }
            case None =>
              dom.console.error("page view for the $num was not added!")
          }
        }

      case Failure(th) =>
        dom.console.error(s"cannot load page $num in paper ${paper.name} with exception ${th}")
        if(retries < 3) seqRender(paper, num, retries +1)
    }
  }

  /**
    * Function that subscribed view updated to the items and selections
    */
  override def subscribeUpdates() = {
    super.subscribeUpdates()
    paper.foreach{
      case EmptyPaper => //do nothing
      case p: PaperPDF => seqRender(p, 1)
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

