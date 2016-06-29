package org.denigma.kappa.messages

import org.denigma.controls.papers.Bookmark


object GoToFigure {

  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[GoToFigure] = boopickle.Default.generatePickler[GoToFigure]
}

case class GoToFigure(figureName: String) extends UIMessage

object GoToPaper {
  /*
    implicit val textLayerSelectionPickler = boopickle.Default.generatePickler[TextLayerSelection]
    implicit val bookmarkPickler = boopickle.Default.generatePickler[Bookmark]
    implicit val goToPickler = boopickle.Default.generatePickler[GoToPaper]
    */
}

case class GoToPaper (bookmark: Bookmark) extends UIMessage
