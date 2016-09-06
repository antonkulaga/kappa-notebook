package org.denigma.kappa.notebook.views.figures

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import scalatags.JsDom.all
import scalatags.JsDom.all._
import org.scalajs.dom.ext._

case class Video(name: String, url: String, text: String) extends Figure

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


