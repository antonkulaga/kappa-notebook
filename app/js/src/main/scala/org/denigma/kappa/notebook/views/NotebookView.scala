package org.denigma.kappa.notebook.views

import org.denigma.binding.binders.{GeneralBinder, _}
import org.denigma.binding.commons.Uploader
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

  val initialCode =
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

  val code = Var(initialCode)
  code.onChange("code change"){ case txt=>
    hub.code() = hub.code.now.copy(text = txt)
  }

  hub.code.onChange("hub changes"){case value =>
    if(value.isEmpty) code.set("") else code.set(value.text)
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

  val onUpload: Var[Event] = Var(Events.createEvent())
  onUpload.onChange("onUpload", uniqueValue = true, skipInitial = true)(ev =>
    this.uploadHandler(ev){
      case Success((file, text))=>
        hub.runParameters.set(hub.runParameters.now.copy(fileName = file.name))
        code.set(text)
      case Failure(th) => dom.console.error(s"File upload failure: ${th.toString}")
    })

   override lazy val injector = defaultInjector
    .register("parameters")((el, args) => new RunnerView(el, hub.runParameters).withBinder(n => new AdvancedBinder(n)))
    .register("results")((el, args) => new ResultsView(el, hub).withBinder(n => new CodeBinder(n)))
}


