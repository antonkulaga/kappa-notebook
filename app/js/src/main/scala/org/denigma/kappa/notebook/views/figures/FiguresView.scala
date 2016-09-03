package org.denigma.kappa.notebook.views.figures

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionMapView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.{GoToFigure, KappaMessage}
import org.denigma.kappa.notebook.views.common.{TabHeaders, TabItem}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable

/**
  * Created by antonkulaga on 08/06/16.
  */
class FiguresView(val elem: Element,
                  val items: Var[Map[String, Figure]],
                  val input: Rx[KappaMessage]
                 ) extends CollectionMapView with TabItem
{

  override type Value = Figure

  override type ItemView = FigureView

  override type Key = String

  val selected = Var("")

  val empty = items.map(its=>its.isEmpty)

  input.onChange {
    case GoToFigure(figure)=>
      items() = items.now.updated(figure.url, figure)
      selected() = figure.url

    case other => //do nothing
  }

  override def newItemView(item: Item, value: Value): ItemView=  this.constructItemView(item){
    case (el, params)=>
      el.id = item
      value match {
        case img: Image =>
          println("IMAGE ="+img.url)
          new ImgView(el, selected, Var(img)).withBinder(v=>new CodeBinder(v))

        case vid: Video if vid.url.contains("youtube") || vid.url.contains(YouTubeView.WATCH) =>
          println("YOUTEUBE = "+vid)
          new YouTubeView(el, selected, Var(vid)).withBinder(v=>new CodeBinder(v))

        case vid: Video =>
          println("VIDEO = "+vid.url)
          new VideoView(el, selected, Var(vid)).withBinder(v=>new CodeBinder(v))

      }
  }

  val headers = itemViews.map(its=> immutable.SortedSet.empty[String] ++ its.values.map(_.id))


  protected def getCaption(url: String): String ={
    url.replace("https://youtube.com/watch?v=", "youtube:")
      .replace("https://www.youtube.com/watch?v=","youtube:")
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected)(getCaption).withBinder(new GeneralBinder(_)))

  override def updateView(view: FigureView, key: String, old: Figure, current: Figure): Unit = {
    //do nothing
  }
}


trait FigureView extends BindableView with TabItem

object Figure {

  import boopickle.Default._
  implicit val classPickler = compositePickler[Figure]
    .addConcreteType[Image]
    .addConcreteType[Video]
}
trait Figure
{
  def name: String
  def url: String
  def text: String
}
