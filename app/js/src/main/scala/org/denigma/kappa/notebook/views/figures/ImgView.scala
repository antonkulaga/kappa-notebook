package org.denigma.kappa.notebook.views.figures

import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

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
}

case class Image(name: String, url: String) extends Figure