package org.denigma.kappa.notebook.views.figures

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionMapView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.{GoToFigure, KappaMessage}
import org.denigma.kappa.notebook.views.annotations.CommentInserter
import org.denigma.kappa.notebook.views.common.{TabHeaders, TabItem}
import org.denigma.kappa.notebook.views.editor.KappaCursor
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable

/**
  * Created by antonkulaga on 08/06/16.
  */
class FiguresView(val elem: Element,
                  val items: Var[Map[String, Figure]],
                  val input: Var[KappaMessage],
                  val kappaCursor: Var[KappaCursor]
                 ) extends CollectionMapView with TabItem with CommentInserter
{

  override type Value = Figure

  override type ItemView = FigureView

  override type Key = String

  val selected = Var("create")

  val empty = items.map(its=>its.isEmpty)


  lazy val comment: Rx[String] = Rx{ //TODO: fix this bad unoptimized code
    val figurePath = selected()
    if(figurePath=="" || figurePath =="create") "" else {
      val addComment = additionalComment()
      val com = if(addComment=="") " " else ":comment \""+ addComment +"\"; "
      items.now(figurePath) match {
        case i: Image => s"#^${com}:image ${toURI(figurePath)}"
        case other => s"#^${com}:video ${toURI(figurePath)}"
      }
    }
  }

  lazy val hasComment = comment.map(com=>com!="")

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
          new ImgView(el, selected, Var(img)).withBinder(v=>new CodeBinder(v))

        case vid: Video if vid.isYouTube =>
          new YouTubeView(el, selected, Var(vid)).withBinder(v=>new CodeBinder(v))

        case vid: Video =>
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
    .register("creator"){(el, args)=>
      el.id = "create"
      new FigureCreator(el, input, selected).withBinder(new GeneralBinder(_))
    }

  override def updateView(view: FigureView, key: String, old: Figure, current: Figure): Unit = {
    //do nothing
  }
}

class FigureCreator(val elem: Element, input: Var[KappaMessage], val selected: Var[String]) extends BindableView with TabItem{

  val url = Var("")
  val description = Var("")
  val addClick = Var(Events.createMouseEvent())
  addClick.triggerLater{
    val str = url.now
    val text = description.now
    input() = GoToFigure(Image(str, str, text))
    url()= ""
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
