package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.messages.{KappaBinaryFile, KappaFile}
import org.denigma.kappa.notebook.views.common.TabItem
import org.denigma.pdf.extensions.Page
import org.scalajs.dom
import org.scalajs.dom.raw
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import org.scalajs.dom.ext._
import org.scalajs.dom.html.Canvas
import org.denigma.kappa.notebook.extensions._
import org.scalajs.dom.raw.Element
import rx.Rx.Dynamic

import scala.annotation.tailrec
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

class PublicationView(val elem: Element,
                      val selected: Var[String],
                      val paper: Var[Paper],
                      kappaCursor: Var[Option[(Editor, PositionLike)]]
                     )
  extends PaperView {

  override type ItemView = ArticlePageView

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
    children.map { case (num, child) => (num, child.select(sel)) }
  }

  lazy val scale = Var(1.4)

  scale.onChange{ value => dom.console.log(s"scale changed to $value")}

  val addNugget= Var(Events.createMouseEvent())

  val paperURI = paper.map(p=>p.name)

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
        seqRender(num + 1, p, 0)

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
    addNugget.triggerLater{
      val curs: Option[(Editor, PositionLike)] = kappaCursor.now
      curs match
      {
        case Some((ed, position)) =>
        //hub.kappaCode() = hub.kappaCode.now.withInsertion(position.line, comments.now)
        case None =>
      }
    }
 }

}

//case class NuggetSelection(paperURI: String, page: Int, selection: TextLayerSelection)

class ArticlePageView(val elem: Element,
                      val num: Int,
                      val page: Page,
                      val scale: Rx[Double]
                     )  extends PageView {
 val name = Var("page_"+num)
 val title = Var("page_"+num)

  def select(sel: Selection): List[TextLayerSelection] = {
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
}