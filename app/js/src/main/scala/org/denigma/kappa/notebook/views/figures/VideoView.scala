package org.denigma.kappa.notebook.views.figures

import org.scalajs.dom.ext._
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scalatags.JsDom.all
import scalatags.JsDom.all._
object Video {
  val WATCH = "watch?v="

  def shorterURL(url: String) = if(url.contains(Video.WATCH)) {
    val i = url.indexOf(Video.WATCH)
    url.substring(i+Video.WATCH.length)
  } else url
}

case class Video(name: String, url: String, text: String) extends Figure
{
  lazy val isYouTube = url.contains("youtube") || url.contains(Video.WATCH)
}

class VideoView(val elem: Element, val selected: Var[String], val video: Var[Video]) extends FigureView
{

  lazy val figureId = this.id + "_figure"

  lazy val text = video.map(v=>v.text)

  lazy val hasText = video.map(vid=>vid.text!="")

  override def bindView() = {
    if(!elem.children.exists(e=>e.id == figureId)) {
      val dataKey = attr("data-bind-src")
      val child = div(all.id := figureId)
      elem.appendChild(child.render)
    }
    super.bindView()
    //<img data-bind-src="src" class="ui huge image">
  }
}


