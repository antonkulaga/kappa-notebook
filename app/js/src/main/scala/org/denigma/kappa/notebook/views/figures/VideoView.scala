package org.denigma.kappa.notebook.views.figures

import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx._

case class Video(name: String, url: String) extends Figure

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


