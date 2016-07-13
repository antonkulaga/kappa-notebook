package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, ItemsMapView}
import org.denigma.codemirror._
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages.ServerMessages.{ParseModel, ServerConnection, SyntaxErrors}
import org.denigma.kappa.messages.WebSimMessages.{WebSimError, WebSimRange}
import org.denigma.kappa.messages.{ServerMessages, Go, KappaFile, KappaMessage}
import org.denigma.kappa.notebook.views.ServerConnections
import org.denigma.kappa.notebook.views.common.TabHeaders
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
      val files = its.values.collect{
        case fl => fl.name -> fl.content
      }.toList
      output() = ServerCommand(connections.now.currentServer, ParseModel(files))
  }

  val selected: Var[String] = Var("")

  override type Item = String

  val syntaxErrors = Var(SyntaxErrors.empty)
  val errorsByFiles: Rx[Map[KappaFile, List[WebSimError]]] =
    syntaxErrors.map{
      case ers =>
        //println("errors are "+ers)
        val byfiles = ers.errorsByFiles()
        //println("BYFILES ARE = "+byfiles)
        byfiles.foldLeft(Map.empty[KappaFile, List[WebSimError]]){
          case (acc, (filename, er)) if items.now.contains(filename) =>
           val fl = items.now(filename)
            val result: Map[KappaFile, List[WebSimError]] = if(acc.contains(fl)) acc.updated(fl, er::acc(fl)) else acc.updated(fl, List(er))
            result

          case (acc, (filename, er)) =>
            dom.console.error(s"received errors for $filename for which the file does not exist!")
            val fl = KappaFile("", filename, "")
            val result: Map[KappaFile, List[WebSimError]] = if(acc.contains(fl)) acc.updated(fl, er::acc(fl)) else acc.updated(fl, List(er))
            result
        }
    }

  val errors: Rx[String] = syntaxErrors.map(er => if(er.isEmpty) "" else er.errors.map(s=>s.message).reduce(_ + "\n" + _))

  val hasErrors = errors.map(e=>e != "")

  val headers = itemViews.map(its=> SortedSet.empty[String] ++ its.values.map(_.id))

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected).withBinder(new GeneralBinder(_)))

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
    require(items.now.contains(key), s"we are adding an Item view for key(${key}) that does not exist")
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
}
