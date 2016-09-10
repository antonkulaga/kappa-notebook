package org.denigma.kappa.messages

import org.denigma.kappa.notebook.parsers.PaperSelection
import org.denigma.kappa.notebook.views.figures.Figure

object GoToFigure {

  import boopickle.DefaultBasic._
  implicit val classPickler: Pickler[GoToFigure] = boopickle.Default.generatePickler[GoToFigure]
}

case class GoToFigure(figure: Figure) extends UIMessage

object GoToPaper {
  /*
    implicit val textLayerSelectionPickler = boopickle.Default.generatePickler[TextLayerSelection]
    implicit val bookmarkPickler = boopickle.Default.generatePickler[Bookmark]
    implicit val goToPickler = boopickle.Default.generatePickler[GoToPaper]
    */
}

case class GoToPaper (paperURI: String) extends UIMessage

case class GoToPaperSelection(selection: PaperSelection, exclusive: Boolean) extends UIMessage
