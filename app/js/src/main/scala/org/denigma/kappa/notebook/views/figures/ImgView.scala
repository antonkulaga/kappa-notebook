package org.denigma.kappa.notebook.views.figures

import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import scalatags.JsDom.all
import scalatags.JsDom.all._
import scala.scalajs.js
import org.scalajs.dom.ext._

/**
  * Created by antonkulaga on 30/06/16.
  */
class ImgView(val elem: Element, val selected: Var[String], val image: Var[Image]) extends FigureView
{
  val src = image.map(i=>"/files/"+i.url)

  val text = image.map(img=>img.text)

  lazy val hasText = image.map(img=>img.text!="")

  lazy val figureId = this.id + "_figure"

  protected def addFigure() = {
    if(!elem.children.exists(e=>e.id == figureId)) {
      val dataKey = attr("data-bind-src")
      val figure = img(all.id := figureId, dataKey := "src",`class` := "ui huge image")
      elem.appendChild(figure.render)

      val link = a(all.id := figureId+"_link", "open in a separate window", target := "_blank",href := this.src.now)
      elem.appendChild(link.render)
    }
  }

  override def bindView() = {
    addFigure()
    super.bindView()
    //<img data-bind-src="src" class="ui huge image">
  }

}

case class Image(name: String, url: String, text: String) extends Figure