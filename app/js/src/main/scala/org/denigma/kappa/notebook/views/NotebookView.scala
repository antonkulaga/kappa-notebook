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
import org.denigma.kappa.messages.KappaMessages
import org.denigma.kappa.notebook.parsers.CommentLinksParser
import org.denigma.kappa.notebook.views.editor.{EditorUpdates, KappaEditor}
import org.denigma.kappa.notebook.{KappaHub, WebSocketTransport}
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html._
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.scalajs.js
import scala.util._
import scalatags.JsDom.all._

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
  subscriber.onOpen.triggerLater{
    connector.send(KappaMessages.Load())
  }

  val code = Var(initialCode)
  code.onChange{ case txt=>
    hub.code() = hub.code.now.copy(text = txt)
  }

  hub.code.onChange{case value =>
    if(value.isEmpty) code.set("") else code.set(value.text)
  }

  val run = Var(org.denigma.binding.binders.Events.createMouseEvent)
  run.triggerLater{
    //dom.console.log("sending the code...")
    connector.send(hub.packContainer())
  }

  val save = Var(Events.createMouseEvent())
  save.triggerLater{
    saveAs(hub.runParameters.now.fileName, code.now)
  }

  val onUpload: Var[Event] = Var(Events.createEvent())
  onUpload.onChange(ev =>
    this.uploadHandler(ev){
      case Success((file, text))=>
        hub.runParameters.set(hub.runParameters.now.copy(fileName = file.name))
        code.set(text)
      case Failure(th) => dom.console.error(s"File upload failure: ${th.toString}")
    })


  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty) //collect updates of all editors together

   override lazy val injector = defaultInjector
     .register("Parameters")((el, args) => new RunnerView(el, hub.runParameters).withBinder(n => new CodeBinder(n)))
     .register("KappaEditor")((el, args) => new KappaEditor(el, code, editorsUpdates))
     .register("Results")((el, args) => new ResultsView(el, hub).withBinder(n => new CodeBinder(n)))

  val commentManager = new CommentsWatcher(editorsUpdates)

}
import org.denigma.codemirror.extensions._

class CommentsWatcher(updates: Var[EditorUpdates])  {

  //val linkParser = P( "a" )
  val commentsParser = new CommentLinksParser().linkAfterComment

  protected def searchForLinks(editor: Editor, line: String, num: Int) = {
    commentsParser.parse(line) match {
      case Parsed.Success(result, index) =>
        val marker = this.makeMarker(result)
        editor.setGutterMarker(num, "breakpoints", marker)

      case Parsed.Failure(parser, index, extra) =>
        editor.setGutterMarker(num, "breakpoints", null) //test setting null
    }
  }


  protected def makeMarker(link: String): Anchor = {
    val tag = a(href := link,
      i(`class` := "file pdf outline icon")
    )
    tag.render

    // <i class="file pdf outline icon">
  }


  protected def changeHandler(upd: EditorUpdates) =
  for {
    editor <- upd.editorOpt
    changed = upd.updates
  }
  {
    val (from, to) = changed.foldLeft( (Int.MaxValue, 0)) {
      case (acc, ch) => ch.mergeSpans(acc)
    }
    val lines = from to to
    //if(lines.nonEmpty)  editor.clearGutter("breakpoints")
    for {
      (num, line) <- editor.linesText(lines)
    } {
      searchForLinks(editor, line , num)
      //val info: LineInfo = editor.lineInfo(num)
      //val markers: js.Array[String] = info.gutterMarkers
    }
  }
}

