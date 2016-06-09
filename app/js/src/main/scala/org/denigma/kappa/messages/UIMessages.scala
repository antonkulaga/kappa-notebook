package org.denigma.kappa.messages

import org.denigma.controls.papers.Bookmark


object GoToPaper {
  /*
    implicit val textLayerSelectionPickler = boopickle.Default.generatePickler[TextLayerSelection]

    implicit val bookmarkPickler = boopickle.Default.generatePickler[Bookmark]

    implicit val goToPickler = boopickle.Default.generatePickler[GoToPaper]
    */
}

case class GoToPaper (bookmark: Bookmark) extends UIMessage