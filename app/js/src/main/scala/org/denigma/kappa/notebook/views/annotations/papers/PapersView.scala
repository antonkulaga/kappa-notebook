package org.denigma.kappa.notebook.views.annotations.papers

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsMapView}
import org.denigma.codemirror.{Editor, PositionLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.papers._
import org.denigma.kappa.notebook.Selector
import org.denigma.kappa.notebook.views.common.TabItem
import org.denigma.kappa.notebook.views.simulations.TabHeaders
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable

class PapersView(val elem: Element,
                 val selected: Var[String],
                 val items: Var[Map[String, Bookmark]],
                 val selector: Selector,
                 kappaCursor: Var[Option[(Editor, PositionLike)]]) extends
  BindableView
  with ItemsMapView
  with TabItem{

  override type Item = String

  override type Value = Bookmark

  override type ItemView = PublicationView

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  override def newItemView(name: String): PublicationView = this.constructItemView(name){
    case (el, params)=>
      el.id = name
      //println("add view "+name)
      val location: Bookmark = this.items.now(name) //buggy but hope it will work
      val v = new PublicationView(el, selector.paper, Var(location), kappaCursor).withBinder(v=>new CodeBinder(v))
      selector.paper() = name
      v
  }


  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selector.paper).withBinder(new GeneralBinder(_)))

  override protected def subscribeUpdates() = {
    super.subscribeUpdates()
    //TODO: move to scalajs binding
    for ( (key, value) <- items.now) {
      val n = newItemView(key)
      n.update(value)
      this.addItemView(key, n)
    }
  }

}


