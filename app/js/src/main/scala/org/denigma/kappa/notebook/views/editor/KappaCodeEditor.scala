package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionMapView}
import org.denigma.codemirror._
import org.denigma.codemirror.addons.lint.{LintFound, LintOptions}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages.ServerMessages.{ParseModel, ServerConnection, SyntaxErrors}
import org.denigma.kappa.messages.WebSimMessages.{Location, WebSimError, WebSimRange}
import org.denigma.kappa.messages.{Go, KappaMessage, KappaSourceFile, ServerMessages}
import org.denigma.kappa.notebook.actions.Movements
import org.denigma.kappa.notebook.views.common.{ServerConnections, TabHeaders}
import org.denigma.kappa.notebook.views.errors.SyntaxErrorsView
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._
import scala.concurrent.duration._
import scala.scalajs.js


class KappaCodeEditor(val elem: Element,
                      val items: Var[Map[String, KappaSourceFile]],
                      val input: Var[KappaMessage],
                      val output: Var[KappaMessage],
                      val kappaCursor: Var[KappaCursor],
                      val editorUpdates: Var[EditorUpdates],
                      val connections: Rx[ServerConnections],
                      val movements: Movements
                     ) extends BindableView
  with CollectionMapView
{

  override type Key = String

  override type Value = KappaSourceFile

  override type ItemView = CodeTab

  val isConnected = connections.map(c=>c.isConnected)

  items.afterLastChange(800 millis){
    its=>
      println("sending files for checking")
      val files: List[(String, String)] = its.values.map{ fl => (fl.name , fl.content) }.toList
      dom.console.log("files to send: "+files.map(nc=>nc._1).mkString(" | "))
      output() = ServerCommand(connections.now.currentServer, ParseModel(files))
  }

  val selected: Var[String] = Var("")


  val syntaxErrors = Var(SyntaxErrors.empty)

  val fullCode = syntaxErrors.map(ers=>ers.fullCode)

  val errorsInFiles: Rx[List[(KappaSourceFile, WebSimError)]] = syntaxErrors.map{ ers => ers.errorsByFiles().flatMap{
    case (filename, er) =>
      if(filename==""){
        val message = "error is out of bounds!"
        dom.console.error(message)
        dom.console.log("all errors "+ers.errors.mkString("\n"))
        dom.console.log("all filenames " + ers.files.map(kv=>kv._1).mkString(" | "))
      }
      if(!items.now.exists(kv=>kv._2.name == filename)) dom.console.error(s"error refers to the $filename that was not found, message: ${er.message}")
      items.now.collect{ case (str, file) if file.name == filename => file -> er }
    }
  }

  val errorsByFiles: Rx[Map[KappaSourceFile, List[WebSimError]]] = errorsInFiles.map{ byfiles => byfiles.groupBy{
        case (key, value) => key
      }.mapValues{ v => v.map(_._2) }
  }

  val headers = itemViews.map(its=> SortedSet.empty[String] ++ its.values.map(v => v.path))

  input.onChange{
    case Go.ToSource(iri, from, to) if iri.local!=""=>
      val its = items.now
      if(its.contains(iri.value)) {

        selected() = iri.value
      }
      else
      if (its.contains(iri.local))
      {
        selected() = iri.local
      }
      else
        {
          its.collectFirst{ case (a, b) if b.name ==iri.name => b.path}.foreach(s=> selected() = s)
        }

    case KappaMessage.ServerResponse(server, ServerMessages.ParseResult(cmap)) => //hide syntax errors when parsing succedded
      syntaxErrors() = SyntaxErrors.empty

    case ServerResponse(server, s: SyntaxErrors) =>
      dom.console.log("syntax errors = "+s)
      syntaxErrors() = s

    case other => //do nothing
  }

  protected def keyVar(key: Key, initialValue: Value): Var[KappaSourceFile] = {
    val v = Var(initialValue)
    v.onChange{ value=>
        items() = items.now.updated(key, value)
    }
    v
    //note: killing should be done on unbinding
  }

  override def newItemView(item: Item, initialValue: Value): ItemView = this.constructItemView(item) {
    case (el, _) =>
      el.id = item //dirty trick
      val value = keyVar(item, initialValue)
      val itemErrors = Rx{
        errorsByFiles().getOrElse(value(), Nil)
      }
      val view: ItemView = new CodeTab(el, item, input, value, selected, editorUpdates, kappaCursor, itemErrors).withBinder(v => new CodeBinder(v) )
      selected() = item
      view
  }
  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected)(TabHeaders.path2name).withBinder(new GeneralBinder(_)))
    .register("SyntaxErrors")((el, args) => new SyntaxErrorsView(el, input, errorsInFiles, fullCode, movements).withBinder(new GeneralBinder(_)))

  override def updateView(view: CodeTab, key: String, old: KappaSourceFile, current: KappaSourceFile): Unit = {
    view.source() = current
  }
}
