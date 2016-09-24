package org.denigma.kappa.notebook.actions

import org.denigma.kappa.messages.FileType.FileType
import org.denigma.kappa.messages.KappaMessage.Container
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.circuits.Circuit
import org.denigma.kappa.notebook.views.MainTabs
import org.denigma.kappa.notebook.views.settings.AnnotationMode
import org.scalajs.dom
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

/**
  * This Circuit is responsible for Animations
  * TODO: make containers compartible with UIMessage
  * @param input
  * @param output
  * @param annotationMode
  */
class AnimationsCircuit(input: Var[KappaMessage], output: Var[KappaMessage],
                        currentProject: Rx[KappaProject],
                        annotationMode: Rx[AnnotationMode.AnnotationMode]) extends Circuit(input, output){

  protected def tabByFileType(fileType: FileType) = fileType match {
    case FileType.pdf =>
      Some(MainTabs.Papers)
    case FileType.video | FileType.image => Some(MainTabs.Figures)
    case FileType.source => Some(MainTabs.Editor)
    case other =>
      dom.console.error("cannot find tab for: "+other)
      None
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

    case Animate(message @ Go.ToFile(file), true) =>  tabByFileType(file.fileType) match {
      case Some(tab) =>
        val cont = annotationContainer(message, tab)
        println(s"annotate tab($tab) with file ${file.path}")
        input() = cont
      case None => dom.console.error(s"unknown filetype ${file.fileType}")
    }

    case Animate(message @ Go.ToFile(file), false) =>  tabByFileType(file.fileType) match {
      case Some(tab) =>
        val goto = goToContainer(message, tab)
        println(s"go to tab($tab) with file ${file.path}")
        input() = goto
      case None => dom.console.error(s"unknown filetype ${file.fileType}")
    }

    case Animate(message @ Go.ToSource(p, _, _), _) =>
      val allSources = currentProject.now.sourceMap
      val keys = allSources.keySet
      allSources.get(p.path).orElse(allSources.collectFirst{case (key, value) if value.name == p.name => value }) match {
        case Some(file) =>
          input() = goToContainer(Go.ToFile(file), MainTabs.Editor).andThen(message)
        case None =>
          input() = goToContainer(message, MainTabs.Editor)
      }

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
