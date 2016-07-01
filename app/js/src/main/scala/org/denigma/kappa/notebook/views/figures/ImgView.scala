package org.denigma.kappa.notebook.views.figures

import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import scalatags.JsDom.all
import scalatags.JsDom.all._
import scalajs.js
import org.scalajs.dom.ext._

/**
  * Created by antonkulaga on 30/06/16.
  */
class ImgView(val elem: Element, val selected: Var[String], val image: Var[Image]) extends FigureView
{
  val src = image.map(i=>"/files/"+i.url)

  override def update(value: Figure) =  value match {
    case v @ Image(name, url)=>
      image() = v
      this

    case _ => dom.console.error("not a valid Image Item")
      this
  }

  lazy val figureId = this.id + "_figure"

  override def bindView() = {
    if(!elem.children.exists(e=>e.id == figureId)) {
      val dataKey = "data-bind-src".attr
      val child = img(all.id := figureId, dataKey := "src", `class` := "ui huge image")
      elem.appendChild(child.render)
    }
    super.bindView()
    //<img data-bind-src="src" class="ui huge image">
  }

}

case class Image(name: String, url: String) extends Figure