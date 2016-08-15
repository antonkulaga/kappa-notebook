package org.denigma.kappa.notebook.actions

import org.denigma.controls.papers.Bookmark
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.parsers.PaperSelection
import org.denigma.kappa.notebook.views.MainTabs


object Movements {

  def toTab(name: String) = Go.ToTab(name)

  def toPaper(paper: String, page: Int) = KappaMessage.Container()
    .andThen(Go.ToTab(MainTabs.Papers))
    .andThen(GoToPaper(paper))
    .copy(betweenInterval = 300)


  def toPaper(paperSelection: PaperSelection) = KappaMessage.Container()
    .andThen(Go.ToTab(MainTabs.Papers))
    .andThen(GoToPaperSelection(paperSelection))
    .copy(betweenInterval = 600)

  def toFigure(figure: String) = KappaMessage.Container()
    .andThen(Go.ToTab(MainTabs.Figures))
    .andThen(GoToFigure(figure))


  def toFile(file: KappaFile):  KappaMessage.Container = file.fileType match {
    case FileType.pdf =>
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Papers))
        .andThen(GoToPaper(file.path))

    case FileType.source =>
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Editor))
        .andThen(Go.ToSource(file.path))

    case FileType.video=>
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Figures))
        .andThen(GoToFigure(file.path))

    case FileType.image=>
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Figures))
        .andThen(GoToFigure(file.path))

    case other => KappaMessage.Container() //do nothing
  }

}
