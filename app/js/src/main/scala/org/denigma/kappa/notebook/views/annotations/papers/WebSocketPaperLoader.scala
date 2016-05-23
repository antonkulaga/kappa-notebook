package org.denigma.kappa.notebook.views.annotations.papers

import org.denigma.controls.papers._
import org.denigma.kappa.messages.FileRequests.LoadFile
import org.denigma.kappa.messages.{DataChunk, DataMessage, FileRequests}
import org.denigma.kappa.notebook.WebSocketTransport
import org.scalajs.dom.raw.{Blob, BlobPropertyBag, FileReader, _}
import rx._

import scala.collection.immutable.{Map, _}
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

case class WebSocketPaperLoader(subscriber: WebSocketTransport, projectName: Rx[String],
                                loadedPapers: Var[Map[String, Paper]] = Var(Map.empty[String, Paper]))
  extends PaperLoader {

  override def getPaper(path: String, timeout: FiniteDuration = 25 seconds) =
  {
    /*
    val tosend: LoadFile = FileRequests.LoadFile(projectName.now, path)
    subscriber.send(tosend)
    val fut = subscriber.collect{
      case d @ DataChunk(message, _, _, _, _,  false) if message == tosend =>
        //println("data")
        d.data
    }{
      case DataChunk(message, _, _, _, _,  true) if message == tosend => true
    }
    val data = fut.map{ case list=> list.reduce(_ ++ _) }
    data.flatMap(bytes=>bytes2Arr(bytes)).flatMap{
      arr=>
        //println("get paper "+path)
        super.getPaper(path, arr)
    }
    */

    val tosend = FileRequests.LoadFileSync(projectName.now, path)
    val result: Future[ArrayBuffer] = subscriber.ask(tosend, 10 seconds){
      case DataMessage(p, bytes) if p.contains(path)=>
        println("RESPONSE!!!!")
        bytes
        } flatMap(bytes=>bytes2Arr(bytes))
    result.flatMap{
      case arr =>
        super.getPaper(path, arr)
    }

  }

  import js.JSConverters._

  def bytes2Arr(data: Array[Byte]): Future[ArrayBuffer] = {
    val p = Promise[ArrayBuffer]
    val options = BlobPropertyBag("octet/stream")
    val arr: Uint8Array = new Uint8Array(data.toJSArray)
    val blob = new Blob(js.Array(arr), options)
    //val url = dom.window.dyn.URL.createObjectURL(blob)
    val reader = new FileReader()
    def onLoadEnd(ev: ProgressEvent): Any = {
      p.success(reader.result.asInstanceOf[ArrayBuffer])
    }
    reader.onloadend = onLoadEnd _
    reader.readAsArrayBuffer(blob)
    p.future
  }

  subscriber.open()

}
