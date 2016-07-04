package org.denigma.kappa.notebook.views.figures

import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import scalatags.JsDom.all
import scalatags.JsDom.all._
import scalajs.js
import org.scalajs.dom.ext._

case class Video(name: String, url: String) extends Figure

class VideoView(val elem: Element, val selected: Var[String], val video: Var[Video]) extends FigureView
{
  //val src = image.map(i => "/files/"+i)

  override def update(value: Figure) =  value match {
    case v @ Video(_, url)=>
      video() = v
      this

    case other => dom.console.error("not a valid Video Item")
      this
  }

  lazy val figureId = this.id + "_figure"

  override def bindView() = {
    if(!elem.children.exists(e=>e.id == figureId)) {
      val dataKey = "data-bind-src".attr
      val child = div(all.id := figureId)
      elem.appendChild(child.render)
    }
    super.bindView()
    //<img data-bind-src="src" class="ui huge image">
  }
}


