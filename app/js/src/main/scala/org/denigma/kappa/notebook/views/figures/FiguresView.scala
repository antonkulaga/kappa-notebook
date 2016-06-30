package org.denigma.kappa.notebook.views.figures

import im.conversant.apps.youtube.{Player, PlayerEvents, PlayerOptions, PlayerVars}
import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsMapView, UpdatableView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.{GoToFigure, KappaMessage}
import org.denigma.kappa.notebook.views.common.{TabHeaders, TabItem}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by antonkulaga on 08/06/16.
  */
class FiguresView(val elem: Element,
                  val items: Var[Map[String, Figure]],
                  val input: Rx[KappaMessage]
                 ) extends ItemsMapView with TabItem
{

  override type Value = Figure

  override type ItemView = FigureView

  override type Item = String

  val selected = Var("")

  val empty = items.map(its=>its.isEmpty)

  input.onChange {
    case GoToFigure(figure)=> selected() = figure
    case other => //do nothing
  }

  override def newItemView(item: Item): ItemView=  this.constructItemView(item){
    case (el, params)=>
      el.id = item
      val value = this.items.now(item) //buggy but hope it will work
      value match {
        case img: Image => new ImgView(el, selected, Var(img)).withBinder(v=>new CodeBinder(v))
        case vid: Video if vid.url.contains("youtube") || vid.url.contains(YouTubeView.WATCH) =>
          new YouTubeView(el, selected, Var(vid)).withBinder(v=>new CodeBinder(v))
        case vid: Video => new VideoView(el, selected, Var(vid)).withBinder(v=>new CodeBinder(v))

      }
  }

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected).withBinder(new GeneralBinder(_)))
}


trait FigureView extends BindableView with TabItem with UpdatableView[Figure]


trait Figure
{
  def name: String
  def url: String
}
