package org.denigma.kappa.notebook.actions

import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.parsers.{AST, PaperSelection}
import org.denigma.kappa.notebook.views.MainTabs
import org.denigma.kappa.notebook.views.figures.{Figure, Image, Video}
import org.denigma.kappa.notebook.views.settings.AnnotationMode
import rx._

class Movements(annotationMode: Rx[AnnotationMode.AnnotationMode]) {

  def toTab(name: String) = Go.ToTab(name)

  def toPaper(paper: String, page: Int) =
    if(annotationMode.now == AnnotationMode.ToAnnotation) {
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Papers))
        .andThen(GoToPaper(paper))
        .copy(betweenInterval = 300)
    } else {
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Editor))
        .andThen(Move.RelativeTo(MainTabs.Editor, MainTabs.Papers, Move.Direction.RIGHT))
        .copy(betweenInterval = 300)
    }

  def toPaper(paperSelection: PaperSelection) =
    if(annotationMode.now == AnnotationMode.ToAnnotation) {
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Papers))
        .andThen(GoToPaperSelection(paperSelection, true))
        .copy(betweenInterval = 300)
    } else {
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Editor))
        .andThen(Move.RelativeTo(MainTabs.Editor, MainTabs.Papers, Move.Direction.RIGHT))
        .andThen(GoToPaperSelection(paperSelection, true))
        .copy(betweenInterval = 200)
    }

  def toFigure(figure: Figure) =
    if(annotationMode.now == AnnotationMode.ToAnnotation)
      KappaMessage.Container()
      .andThen(Go.ToTab(MainTabs.Figures))
      .andThen(GoToFigure(figure))
    else {
      KappaMessage.Container()
      .andThen(Go.ToTab(MainTabs.Editor))
      .andThen(Move.RelativeTo(MainTabs.Editor, MainTabs.Figures, Move.Direction.RIGHT))
      .andThen(GoToFigure(figure))
    }

  def toSource(iri: AST.IRI, begin: Int, end: Int) =
    KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Editor))
        .andThen(Go.ToSource(iri, begin, end ))
        .copy(betweenInterval = 200)


  def toFile(file: KappaFile):  KappaMessage.Container = file.fileType match {
    case FileType.pdf =>
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Papers))
        .andThen(GoToPaper(file.path))

    case FileType.source =>
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Editor))
        .andThen(Go.ToSource(AST.IRI(file.path)))

    case FileType.video=>
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Figures))
        .andThen(GoToFigure(Video(file.name, file.path, "")))

    case FileType.image=>
      KappaMessage.Container()
        .andThen(Go.ToTab(MainTabs.Figures))
        .andThen(GoToFigure(Image(file.name, file.path, "")))

    case other => KappaMessage.Container() //do nothing
  }

}
