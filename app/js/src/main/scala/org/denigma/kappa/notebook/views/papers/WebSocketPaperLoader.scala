package org.denigma.kappa.notebook.views.papers

import org.denigma.controls.papers._
import org.denigma.kappa.messages.FileRequests.LoadFileSync
import org.denigma.kappa.messages.{FileRequests, KappaBinaryFile}
import org.denigma.kappa.notebook.WebSocketTransport
import org.denigma.pdf.PDFJS
import org.denigma.pdf.extensions._
import rx._

import scala.collection.immutable.Map
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray.ArrayBuffer

case class WebSocketPaperLoader(subscriber: WebSocketTransport, projectName: Rx[String],
                                loadedPapers: Var[Map[String, Paper]] = Var(Map.empty[String, Paper]))
  extends PaperLoader {

  override def getPaper(path: String, timeout: FiniteDuration = 25 seconds): Future[Paper] =
  {
    val tosend: LoadFileSync = FileRequests.LoadFileSync(projectName.now, path)
    val result: Future[ArrayBuffer] = subscriber.ask(tosend, 10 seconds){
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
