package org.denigma.kappa.notebook.views.figures
/*
import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsMapView, UpdatableView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.notebook.Selector
import org.denigma.kappa.notebook.views.common.{TabHeaders, TabItem}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable

class ImagesView(val elem: Element,
                 val selected: Var[String],
                 val selector: Selector,
                 val items: Var[Map[String, String]]) extends ItemsMapView with TabItem
{

  println("ITEMS = "+items.now)
  override type Value = String

  override type ItemView = ImgView

  override type Item = String

  override def newItemView(item: Item): ItemView=  this.constructItemView(item){
    case (el, params)=>
      el.id = item
      //println("add view "+name)
      val value = this.items.now(item) //buggy but hope it will work
      val v = new ImgView(el, selector.image, Var(value)).withBinder(v=>new CodeBinder(v))
      selector.image() = item
      v
  }

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selector.image).withBinder(new GeneralBinder(_)))

}

class ImgView(val elem: Element, val selected: Var[String], val image: Var[String]) extends BindableView with TabItem with UpdatableView[String]
{
  val src = image.map(i => "/files/"+i)

  override def update(value: String): ImgView.this.type = {
    image() = value
    this
  }
}
*/