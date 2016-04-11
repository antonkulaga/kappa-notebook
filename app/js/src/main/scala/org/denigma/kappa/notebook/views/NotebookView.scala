package org.denigma.kappa.notebook.views

import fastparse.all._
import org.denigma.binding.binders._
import org.denigma.binding.commons.Uploader
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.{Editor, EditorChangeLike}
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.sockets.WebSocketSubscriber
import org.denigma.kappa.notebook.views.editor.{CommentsWatcher, EditorUpdates, KappaCodeEditor}
import org.denigma.kappa.notebook.{KappaHub, WebSocketTransport}
import org.scalajs.dom.raw.{Element, HTMLElement}
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import org.denigma.kappa.notebook.views.visual.GraphView

class NotebookView(val elem: Element, val session: Session) extends BindableView
{
  self =>


  lazy val subscriber = WebSocketSubscriber("notebook", "guest" + Math.random() * 1000)

  val hub: KappaHub = KappaHub.empty


  val connector: WebSocketTransport = WebSocketTransport(subscriber, hub)

    subscriber.onOpen.triggerLater{
      println("websocket opened")
      //connector.send(WebSim.Load("model.ka"))
    }




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

    val code = Var(initialCode)
    code.onChange{ case txt=>
      hub.kappaCode() = hub.kappaCode.now.copy(text = txt)
    }

    hub.kappaCode.onChange{case v =>
      if(v.isEmpty) code.set("") else code.set(v.text)
    }

    hub.runParameters.triggerLater{
      connector.send(hub.runParameters.now)
    }
    val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty) //collect updates of all editors together

    val commentManager = new CommentsWatcher(editorsUpdates, hub.paperLocation)

     override lazy val injector = defaultInjector
       .register("KappaEditor")((el, args) => new KappaCodeEditor(el, hub, editorsUpdates).withBinder(n => new CodeBinder(n)))
       .register("Tabs")((el, args) => new TabsView(el, hub).withBinder(n => new CodeBinder(n)))
       .register("GraphView") {  (el, args) => new GraphView(el).withBinder(n => new CodeBinder(n))
       }
}



