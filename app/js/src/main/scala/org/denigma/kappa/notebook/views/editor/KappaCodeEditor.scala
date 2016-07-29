package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsMapView}
import org.denigma.codemirror._
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages.ServerMessages.{ParseModel, ServerConnection, SyntaxErrors}
import org.denigma.kappa.messages.WebSimMessages.{Location, WebSimError, WebSimRange}
import org.denigma.kappa.messages.{Go, KappaFile, KappaMessage, ServerMessages}
import org.denigma.kappa.notebook.views.common.{ServerConnections, TabHeaders}
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import scala.collection.immutable._
import scala.concurrent.duration._


class KappaCodeEditor(val elem: Element,
                      val items: Var[Map[String, KappaFile]],
                      val input: Var[KappaMessage],
                      val output: Var[KappaMessage],
                      val kappaCursor: Var[Option[(Editor, PositionLike)]],
                      val editorUpdates: Var[EditorUpdates],
                      val connections: Rx[ServerConnections]
                     ) extends BindableView
  with ItemsMapView
{

  override type Value = KappaFile

  override type ItemView = CodeTab

  val isConnected = connections.map(c=>c.isConnected)

  items.afterLastChange(800 millis){
    its=>
      //println("sending files for checking")
      val files: List[(String, String)] = its.values.map{  case fl => (fl.name , fl.content) }.toList
      dom.console.log("files to send: "+files.map(nc=>nc._1).mkString(" | "))
      output() = ServerCommand(connections.now.currentServer, ParseModel(files))
  }

  val selected: Var[String] = Var("")

  override type Item = String

  val syntaxErrors = Var(SyntaxErrors.empty)

  val fullCode = syntaxErrors.map(ers=>ers.fullCode)

  val errorsInFiles: Rx[List[(KappaFile, WebSimError)]] = syntaxErrors.map{ case ers => ers.errorsByFiles().flatMap{
    case (filename, er) =>
      if(filename==""){
        val message = "error is out of bounds!"
        dom.console.error(message)
        dom.console.log("all errors "+ers.errors.mkString("\n"))
        dom.console.log("all filenames " + ers.files.map(kv=>kv._1).mkString(" | "))
      }
      if(!items.now.contains(filename)) dom.console.error(s"error refers to the $filename that was not found")
      items.now.collect{ case (str, file) if file.name == filename => file -> er }
    }
  }

  val errorsByFiles: Rx[Map[KappaFile, List[WebSimError]]] = errorsInFiles.map
  {
    case byfiles => byfiles.groupBy{
        case (key, value) => key
      }.mapValues{
        case v => v.map(_._2)
      }
  }

  val headers = itemViews.map(its=> SortedSet.empty[String] ++ its.values.map(v => v.path))

  input.onChange{
    case Go.ToSource(name, from, to)=>
      selected() = name

    case KappaMessage.ServerResponse(server, ServerMessages.ParseResult(cmap)) => //hide syntax errors when parsing succedded
      syntaxErrors() = SyntaxErrors.empty

    case ServerResponse(server, s: SyntaxErrors) =>
      syntaxErrors() = s

    case other => //do nothing
  }

  protected def keyVar(key: Key) = {
    //require(items.now.contains(key), s"we are adding an Item view for key(${key}) that does not exist")
    val initialValue = this.items.now(key)
    val v = Var(initialValue)
    v.onChange{
      case value=>
        items() = items.now.updated(key, value)
    }
    v
    //note: killing should be done on unbinding
  }

  override def newItemView(item: Item): ItemView = this.constructItemView(item) {
    case (el, _) =>
      el.id = item //dirty trick
      val value: Var[KappaFile] = keyVar(item)
      val itemErrors = Rx{
        errorsByFiles().getOrElse(value(), Nil)
      }
      val view: ItemView = new CodeTab(el, item, value, selected, editorUpdates, kappaCursor, itemErrors).withBinder(v => new CodeBinder(v) )
      selected() = item
      view
  }

  def path2name(path: String) = path.lastIndexOf("/") match {
    case -1 => path
    case ind if ind == path.length -1 => path
    case ind => path.substring(ind+1)
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected)(path2name).withBinder(new GeneralBinder(_)))
    .register("errors")((el, args) => new ErrorsView(el, input, errorsInFiles, fullCode).withBinder(new GeneralBinder(_)))

}
