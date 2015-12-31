package org.denigma.kappa.notebook.views

import org.denigma.binding.extensions._
import org.denigma.binding.views._
import org.denigma.controls.login.Session
import org.denigma.controls.sockets.WebSocketSubscriber
import org.denigma.kappa.messages.KappaMessages
import org.denigma.kappa.notebook.{KappaHub, WebSocketTransport}
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.core._
import rx.ops._

class NotebookView(val elem: Element, val session: Session) extends BindableView
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
    if(value.text!=code.now) { if(value.isEmpty) code() = defaultCode else value.text }
  }

  val send = Var(org.denigma.binding.binders.Events.createMouseEvent)
  send.handler{
    dom.console.log("sending the code...")
    connector.send(hub.packContainer())
  }

   override lazy val injector = defaultInjector
    .register("parameters")((el, args) => new RunnerView(el, hub.runParameters).withBinder(n => new AdvancedBinder(n)))
    .register("results")((el, args) => new ResultsView(el, hub).withBinder(n => new AdvancedBinder(n)))
}