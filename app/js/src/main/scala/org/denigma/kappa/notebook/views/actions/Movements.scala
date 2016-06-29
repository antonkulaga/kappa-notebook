package org.denigma.kappa.notebook.views.actions

import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.views.MainTabs
import rx.Rx.Dynamic
import rx.{Rx, Var}


object Movements {

  def toTab(name: String) = Go.ToTab(name)

  def toPaper(paper: String, page: Int) = KappaMessage.Container()
    .andThen(Go.ToTab(MainTabs.Papers))
    .andThen(GoToPaper(Bookmark(paper, page)))

  def toFigure(figure: String) = KappaMessage.Container()
    .andThen(Go.ToTab(MainTabs.Figures))
    .andThen(GoToFigure(figure))

}
