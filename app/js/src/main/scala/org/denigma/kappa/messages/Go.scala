package org.denigma.kappa.messages

import boopickle.Pickler
import boopickle.DefaultBasic._
import org.denigma.kappa.notebook.parsers.{AST, PaperSelection}
import org.denigma.kappa.notebook.views.figures.Figure

object Go {
  object ToTab {
    implicit val classPickler: Pickler[ToTab] = boopickle.Default.generatePickler[ToTab]
  }
  case class ToTab(name: String) extends UIMessage

  object ToSource {
    implicit val sourcePickler: Pickler[ToSource] = boopickle.Default.generatePickler[ToSource]
  }

  case class ToSource(path: AST.IRI, begin: Int = 0, end: Int = 0) extends UIMessage

  case class ToFile(file: KappaFile) extends UIMessage

  object ToPaper{
    implicit val classPickler: Pickler[ToPaper] = boopickle.Default.generatePickler[ToPaper]
  }

  case class ToPaper(paper: String, page: Int) extends UIMessage

  case class ToPaperSelection(selection: PaperSelection, exclusive: Boolean) extends UIMessage

  case class ToFigure(figure: Figure) extends UIMessage

}