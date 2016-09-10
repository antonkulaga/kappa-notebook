package org.denigma.kappa.notebook.views.papers

import org.denigma.controls.papers._
import org.denigma.kappa.messages.FileRequests.LoadFileSync
import org.denigma.kappa.messages.{FileRequests, KappaBinaryFile}
import org.denigma.kappa.notebook.WebSocketTransport
import org.denigma.pdf.PDFJS
import org.denigma.pdf.extensions._
import rx._

import scala.Predef.Map
import scala.collection.immutable._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray.ArrayBuffer

case class WebSocketPaperLoader(subscriber: WebSocketTransport,
                                projectPapers: Rx[Map[String, KappaBinaryFile]],
                                loadedPapers: Var[Map[String, Paper]] = Var(Map.empty[String, Paper]))
  extends PaperLoader {

  override def getPaper(path: String, timeout: FiniteDuration = 25 seconds): Future[Paper] =
  {
    if(loadedPapers.now.contains(path)) Future.successful(loadedPapers.now(path)) else
      projectPapers.now.get(path) match {
        case Some(f) if f.isEmpty => send(path)(timeout)
        case Some(f) =>
          val data: ArrayBuffer = subscriber.bytes2message(f.content)
          PDFJS.getDocument(data).toFuture.map{ proxy =>
            val paper = Paper(path, proxy)
            //note - we do not update cache to avoid side effects
            paper
          }
        case None => send(path)(timeout)
      }
  }

  protected def send(path: String)(implicit timeout: FiniteDuration): Future[Paper] = {
    val tosend: LoadFileSync = FileRequests.LoadFileSync(path)
    val result: Future[ArrayBuffer] = subscriber.ask(tosend, timeout){
      case KappaBinaryFile(p, bytes, _, _) if p.contains(path)=>
        subscriber.bytes2message(bytes)
    }
    result.flatMap{ data =>
      PDFJS.getDocument(data).toFuture.map{ proxy =>
        val paper = Paper(path, proxy)
        //note - we do not update cache to avoid side effects
        paper
      }
    }
  }

  subscriber.open()

}

/*
case class AjaxPaperLoader(files: String = "/files/",
                           projectPapers: Rx[Map[String, KappaBinaryFile]],
                           loadedPapers: Var[Map[String, Paper]] = Var(Map.empty[String, Paper])) extends PaperLoader {

  override def getPaper(path: String, timeout: FiniteDuration): Future[Paper] = {
    if (loadedPapers.now.contains(path)) Future.successful(loadedPapers.now(path))
    else
      projectPapers.now.get(path) match {
        case Some(f) if !f.isEmpty =>
        case _ => load(path, timeout)

      }
  }

  protected def load(path: String, timeout: FiniteDuration) = {
      val url = if(path.contains(":"))  path else files + path
      PDFJS.getDocument(url).toFuture.map{ proxy =>
      val paper = Paper(path, proxy)
      //note - we do not update cache to avoid side effects
      paper
    }
  }

}
*/