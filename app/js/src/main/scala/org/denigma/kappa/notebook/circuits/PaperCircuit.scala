package org.denigma.kappa.notebook.circuits

import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.WebSocketTransport
import org.denigma.kappa.notebook.actions.Commands
import org.denigma.kappa.notebook.parsers.PaperSelection
import org.denigma.kappa.notebook.views.papers.WebSocketPaperLoader
import org.scalajs.dom
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.duration._
import org.denigma.binding.extensions._
import org.denigma.controls.papers.PaperLoader

import scala.util.{Failure, Success}

class PaperCircuit(input: Var[KappaMessage], output: Var[ KappaMessage], paperLoader: PaperLoader) extends Circuit(input, output){


  def papers = paperLoader.loadedPapers

  val paperURI = Var("")
  val paperSelectionOpt: Var[Option[Go.ToPaperSelection]] = Var(None)

  paperURI.foreach{
    case paper if paper!="" =>
      //println("LOAD :"+loc.paper)
      paperLoader.getPaper(paper, 10 seconds).onComplete{
        case Success(pp) => paperLoader.loadedPapers() = paperLoader.loadedPapers.now.updated(pp.name, pp)
        case Failure(th)=> dom.console.error(s"Cannot load paper ${paper}: "+th)
      }
    case _ => //do nothing
  }



  override protected def onInputMessage(message: KappaMessage): Unit = message match {
  
    case Go.ToFile(b: KappaBinaryFile) if b.fileType == FileType.pdf =>
      //println("GO TO EMPTY PAPER IS DETECTED")
      paperURI() = b.path
  
    case Go.ToPaper(loc, _)=>
      //println(s"go to paper ${loc}")
      paperURI() = loc //just switches to another paper
  
    case message @ Go.ToPaperSelection(selection, exc) =>
      //println("GO TO PAPER SELECTION")
      paperURI.Internal.value = selection.label //TODO: fix this ugly workaround
  
      paperLoader.getPaper(selection.label, 12 seconds).onComplete{
        case Success(paper) =>
          paperLoader.loadedPapers() = paperLoader.loadedPapers.now.updated(paper.name, paper)
          paperSelectionOpt() = Some(message)

        case Failure(th)=> dom.console.error(s"Cannot load paper ${selection.label}: "+th)
      }
  
    case  Commands.CloseFile(path) =>
        if(papers.now.contains(path)) {
        papers() = papers.now - path
        } else {
          //dom.console.error(s"cannot find ${path}")
        }

    case _ => //do nothing
  }
}
