package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{GeneralBinder, _}
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.sockets.WebSocketSubscriber
import org.denigma.kappa.messages.KappaMessages
import org.denigma.kappa.notebook.{KappaHub, WebSocketTransport}
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.{Element, FileReader}
import rx.core._
import org.denigma.binding.extensions._
import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}
import scala.util._
import scalajs.concurrent.JSExecutionContext.Implicits.queue

class NotebookView(val elem: Element, val session: Session) extends BindableView with Uploader
{
  self =>

  lazy val subscriber = WebSocketSubscriber("notebook", "guest")

  val hub: KappaHub = KappaHub.empty

  val defaultCode =
    """
      |####### ADD YOUR CODE HERE #############
      |
      |#### Signatures
      |
      |#### Rules
      |
      |#### Variables
      |
      |#### Observables
      |
      |#### Initial conditions
      |
      |#### Modifications
    """.stripMargin

  val connector: WebSocketTransport = WebSocketTransport(subscriber, hub)
  subscriber.onOpen.handler{
    connector.send(KappaMessages.Load())
  }

  val code = Var(defaultCode)
  code.onChange("code change"){ case txt=>
    hub.code() = hub.code.now.copy(text = txt)
  }

  hub.code.onChange("hub changes"){case value =>
    if(value.isEmpty) code.set(defaultCode) else code.set(value.text)
  }

  val run = Var(org.denigma.binding.binders.Events.createMouseEvent)
  run.handler{
    //dom.console.log("sending the code...")
    connector.send(hub.packContainer())
  }

  val save = Var(Events.createMouseEvent())
  save.handler{
    saveAs(hub.runParameters.now.fileName, code.now)
  }


  val onUpload = Var(Events.createEvent())
  onUpload.onChange("onUpload", uniqueValue = true, skipInitial = true){case ev =>
    if(ev.target == ev.currentTarget){
      ev.preventDefault()
      println("on upload works!")
      ev.target match {
        case input: Input =>
          val files: List[File] = input.files
          for(f <- files) {
            val reader = new FileReader()
            reader.readAsText(f)
            val fut = readText(f)
            fut.onComplete{
              case Success(file)=> code.set(file)
              case Failure(th) => dom.console.error(s"File upload failure: ${th.toString}")
            }
          }
        case null => println("null file input")
        case _ => dom.console.error("not a file input")
      }
    }
  }

   override lazy val injector = defaultInjector
    .register("parameters")((el, args) => new RunnerView(el, hub.runParameters).withBinder(n => new GeneralBinder(n)))
    .register("results")((el, args) => new ResultsView(el, hub).withBinder(n => new CodeBinder(n)))
}


trait Uploader {

  @tailrec final def filesToList(f: FileList, acc: List[File] = Nil, num: Int = 0): List[File] = {
    if (f.length <= num) acc.reverse else filesToList(f, f.item(num)::acc, num + 1)
  }

  implicit def filesAsList(f: FileList): List[File] = filesToList(f, Nil, 0)

  protected def readText(f: File): Future[String] = {
    val result = Promise[String]
    val reader = new FileReader()
    def onLoadEnd(ev: ProgressEvent): Any = {
      result.success(reader.result.toString)
    }
    def onErrorEnd(ev: Event): Any = {
      result.failure(new Exception("READING FAILURE " + ev.toString))
    }
    reader.onloadend = onLoadEnd _
    reader.onerror = onErrorEnd _
    reader.readAsText(f)
    result.future
  }
}