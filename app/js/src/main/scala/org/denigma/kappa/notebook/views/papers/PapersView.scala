package org.denigma.kappa.notebook.views.papers

import org.denigma.binding.binders.Events
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.querki.jquery.$
import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._


class PapersView(val elem: Element, selected: Rx[String]) extends Annotator {


  val active: rx.Rx[Boolean] = selected.map(value => value == this.id)

  //start location to run
  val location = Var(Bookmark("/resources/pdf/eptcs.pdf", 1))
  val paperURI = location.map(_.paper)


  val canvas: Canvas  = $("#the-canvas").get(0).asInstanceOf[Canvas]
  //val $textLayerDiv: JQuery = $("#text-layer")
  val textLayerDiv: Element = dom.document.getElementById("text-layer")//.asInstanceOf[HTMLElement]

  //val paperName = paperManager.currentPaper.map(_.name)


  val nextPage = Var(Events.createMouseEvent())
  val previousPage = Var(Events.createMouseEvent())


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
    super.subscribePapers()
  }

  /**
    * Register views
    */
  override lazy val injector = defaultInjector
    .register("Bookmarks"){
      case (el, args) =>  new BookmarksView(el, location, textLayerDiv).withBinder(new CodeBinder(_))
    }

 }