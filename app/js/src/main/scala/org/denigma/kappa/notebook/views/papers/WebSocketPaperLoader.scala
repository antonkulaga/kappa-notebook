package org.denigma.kappa.notebook.views.papers

import org.denigma.controls.papers._
import org.denigma.kappa.messages.FileRequests.{LoadFileSync, LoadBinaryFile}
import org.denigma.kappa.messages.{DataChunk, DataMessage, FileRequests}
import org.denigma.kappa.notebook.WebSocketTransport
import org.denigma.pdf.PDFJS
import org.scalajs.dom.raw.{Blob, BlobPropertyBag, FileReader, _}
import rx._

import scala.collection.immutable.{Map, _}
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import org.denigma.binding.extensions._
import org.denigma.pdf.extensions._
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

case class WebSocketPaperLoader(subscriber: WebSocketTransport, projectName: Rx[String],
                                loadedPapers: Var[Map[String, Paper]] = Var(Map.empty[String, Paper]))
  extends PaperLoader {

  override def getPaper(path: String, timeout: FiniteDuration = 25 seconds): Future[Paper] =
  {
    val tosend: LoadFileSync = FileRequests.LoadFileSync(projectName.now, path)
    val result: Future[ArrayBuffer] = subscriber.ask(tosend, 10 seconds){
      case DataMessage(p, bytes) if p.contains(path)=>
        bytes
        }.flatMap(bytes=>bytes2Arr(bytes))
    result.flatMap{
      case data =>
        PDFJS.getDocument(data).toFuture.map{
          case proxy =>
            val paper = Paper(path, proxy)
            //note - we do not update cache to avoid side effects
            paper
        }
    }
  }

  import js.JSConverters._

  subscriber.open()

}
