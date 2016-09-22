package org.denigma.kappa.notebook.actions

import org.denigma.kappa.messages.FileType.FileType
import org.denigma.kappa.messages.KappaMessage.Container
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.circuits.Circuit
import org.denigma.kappa.notebook.views.MainTabs
import org.denigma.kappa.notebook.views.settings.AnnotationMode
import org.scalajs.dom
import rx._

class Movements(input: Var[KappaMessage], output: Var[KappaMessage], annotationMode: Rx[AnnotationMode.AnnotationMode]) extends Circuit(input, output){



  /*
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

    case other => KappaMessage.Container() //do nothing
  }
  */

  protected def tabByFileType(fileType: FileType) = fileType match {
    case FileType.pdf => Some(MainTabs.Papers)
    case FileType.video | FileType.image => Some(MainTabs.Figures)
    case FileType.source => Some(MainTabs.Editor)
    case other => None
  }

  protected def goToContainer(message: KappaMessage, tab: String) =  KappaMessage.Container()
    .andThen(Go.ToTab(tab))
    .andThen(message)

  protected def goToRightContainer(message: KappaMessage, tab: String) =   KappaMessage.Container()
    .andThen(Go.ToTab(MainTabs.Editor))
    .andThen(Move.RelativeTo(MainTabs.Editor, tab, Move.Direction.RIGHT)).andThen(message)

  def annotationContainer(message: KappaMessage, tab: String): Container =
    if(annotationMode.now == AnnotationMode.ToAnnotation) goToContainer(message, tab) else goToRightContainer(message, tab)

  override protected def onInputMessage(message: KappaMessage): Unit = message match {

    case Animate(Go.ToFile(file), true) =>  tabByFileType(file.fileType).foreach{ tab => input() = annotationContainer(Go.ToFile(file), tab)}

    case Animate(Go.ToFile(file), false) =>  tabByFileType(file.fileType).foreach{ tab => input() = goToContainer(Go.ToFile(file), tab)}

    case Animate(message: Go.ToSource, _) => goToContainer(message, MainTabs.Editor)

    case Animate(message: Go.ToPaperSelection, true) => input()=  annotationContainer(message, MainTabs.Papers).copy(betweenInterval = 300)

    case Animate(message: Go.ToPaperSelection, false) => input()=  goToContainer(message, MainTabs.Papers).copy(betweenInterval = 300)

    case Animate(message: Go.ToFigure, true) => input() = annotationContainer(message, MainTabs.Figures)

    case Animate(message: Go.ToFigure, false) => input() = goToContainer(message, MainTabs.Figures)

    case Animate(message: Go.ToPaper, true) => input() = annotationContainer(message, MainTabs.Papers).copy(betweenInterval = 300)

    case Animate(message: Go.ToPaper, false) => input() = goToContainer(message, MainTabs.Papers).copy(betweenInterval = 300)

    case Animate(other, _) => dom.console.error(s"Unknown Animate message: ${other}")

    case other => //do nothing
  }
}
