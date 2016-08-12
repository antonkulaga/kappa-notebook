package org.denigma.kappa.messages

import org.denigma.kappa.notebook.parsers.PaperSelection

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

case class GoToPaper (paperURI: String) extends UIMessage

case class GoToPaperSelection(selection: PaperSelection) extends UIMessage
