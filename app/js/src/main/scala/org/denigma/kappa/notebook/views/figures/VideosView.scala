package org.denigma.kappa.notebook.views.figures
/*
import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsMapView, UpdatableView}
import org.denigma.kappa.notebook.views.common.{TabHeaders, TabItem}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable

/**
  * Created by antonkulaga on 5/10/16.
  */
class VideosView(val elem: Element, val items: Rx[Map[String, String]], val selected: Var[String]) extends BindableView
  with ItemsMapView
  with TabItem
{

  val selectedVideo = Var("")

  override type Item = String

  override type Value = String

  override type ItemView = VidView

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  override def newItemView(name: Key): ItemView = this.constructItemView(name)
  {
    case (el, params)=>
      el.id = name
      //println("add view "+name)
      val value = this.items.now(name) //buggy but hope it will work
      /*
      val v = new PublicationView(el, selector.paper, Var(location), kappaCursor).withBinder(v=>new CodeBinder(v))
      selector.paper() = name
      v
      */
      val v = new VidView(el, selected, Var(name))
      v.update(value)
      v
  }
  /*
  override def newItemView(item: String): ItemView = this.constructItemView(item){
    case (el, params)=>
      el.id = item
      val v = new VidView(el, selected, item)
      v
  }
  */

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selectedVideo).withBinder(new GeneralBinder(_)))

}

class VidView(val elem: Element, val selected: Var[String], val src: Var[String]) extends BindableView with TabItem with UpdatableView[String]
{
  override def update(value: String): VidView.this.type = {
    src() = value
    this
  }
}
*/