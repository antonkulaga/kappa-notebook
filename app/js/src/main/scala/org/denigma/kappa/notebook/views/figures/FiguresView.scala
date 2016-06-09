package org.denigma.kappa.notebook.views.figures

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.{BindableView, ItemsMapView, UpdatableView}
import org.denigma.controls.code.CodeBinder
import org.scalajs.dom
import scala.collection.immutable
import org.denigma.kappa.notebook.views.common.{TabHeaders, TabItem}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
/**
  * Created by antonkulaga on 08/06/16.
  */
class FiguresView(val elem: Element,
                  val selected: Var[String],
                  val items: Var[Map[String, Figure]]) extends ItemsMapView with TabItem
{

  override type Value = Figure

  override type ItemView = FigureView

  override type Item = String

  val empty = items.map(its=>its.isEmpty)

  override def newItemView(item: Item): ItemView=  this.constructItemView(item){
    case (el, params)=>
      el.id = item
      val value = this.items.now(item) //buggy but hope it will work
      value match {
        case img: Image => new ImgView(el, selected, Var(img)).withBinder(v=>new CodeBinder(v))
        case vid: Video => new VideoView(el, selected, Var(vid)).withBinder(v=>new CodeBinder(v))
      }
  }

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected).withBinder(new GeneralBinder(_)))

}

class ImgView(val elem: Element, val selected: Var[String], val image: Var[Image]) extends FigureView
{
  val src = image.map(i => "/files/"+i)

  override def update(value: Figure) =  value match {
    case v @ Image(name, url)=>
      image() = v
      this

    case _ => dom.console.error("not a valid Image Item")
      this
  }
}

class VideoView(val elem: Element, val selected: Var[String], val video: Var[Video]) extends FigureView
{
  //val src = image.map(i => "/files/"+i)

  override def update(value: Figure) =  value match {
    case v @ Video(name, url)=>
      video() = v
      this

    case other => dom.console.error("not a valid Video Item")
      this
  }
}

trait FigureView extends BindableView with TabItem with UpdatableView[Figure]

case class Image(name: String, url: String) extends Figure
case class Video(name: String, url: String) extends Figure

trait Figure
{
  def name: String
  def url: String
}
