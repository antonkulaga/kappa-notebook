package org.denigma.kappa.notebook.views


import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.controls.login.Session
import org.denigma.controls.sockets.WebSocketSubscriber
import org.denigma.kappa.messages.LaunchModel
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.Side
import org.denigma.kappa.notebook.views.editor.{CommentsWatcher, EditorUpdates, KappaCodeEditor, KappaWatcher}
import org.denigma.kappa.notebook.views.project.FilesView
import org.denigma.kappa.notebook.views.visual._
import org.denigma.kappa.notebook.{KappaHub, WebSocketTransport}
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import SvgBundle.all._
import SvgBundle.all.attrs._


class NotebookView(val elem: Element, val session: Session) extends BindableView with InitialCode
{
  self =>

  lazy val subscriber = WebSocketSubscriber("notebook", "guest" + Math.random() * 1000)

  val hub: KappaHub = KappaHub.empty

  val connector: WebSocketTransport = WebSocketTransport(subscriber, hub)

    subscriber.onOpen.triggerLater{
      println("websocket opened")
      //connector.send(WebSim.Load("model.ka"))
    }

    val code = Var(initialCode)
    code.onChange{ case txt=>
      hub.kappaCode() = hub.kappaCode.now.copy(text = txt)
    }

    hub.kappaCode.onChange{case v =>
      if(v.isEmpty) code.set("") else code.set(v.text)
    }

    hub.runParameters.triggerLater{
      connector.send(LaunchModel("", hub.runParameters.now))
    }

    val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty) //collect updates of all editors together

    val commentManager = new CommentsWatcher(editorsUpdates, hub.papers)

    lazy val s: SVG = {
      val t = svg(/*width :=  defaultWidth, height := defaultHeight*/).render
      t.style.position = "absolute"
      t.style.top = "-9999"
      elem.appendChild(t)
      t
    }

  val kappaWatcher = new KappaWatcher(hub.kappaCursor, editorsUpdates, s)

   override lazy val injector = defaultInjector
     .register("KappaEditor")((el, args) => new KappaCodeEditor(el, hub, editorsUpdates).withBinder(n => new CodeBinder(n)))
     .register("Tabs")((el, args) => new TabsView(el, hub).withBinder(n => new CodeBinder(n)))
     .register("GraphView") {  (el, args) => new GraphView(el, kappaWatcher.nodes, kappaWatcher.edges, kappaWatcher.layouts).withBinder(n => new CodeBinder(n)) }
     .register("Files") {  (el, args) => new FilesView(el, hub.path).withBinder(n => new CodeBinder(n)) }

}


trait InitialCode {
  lazy val initialCode =
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

}

