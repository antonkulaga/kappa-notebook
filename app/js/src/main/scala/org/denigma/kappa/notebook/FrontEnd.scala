package org.denigma.kappa.notebook

import org.denigma.binding.binders.{GeneralBinder, NavigationBinder}
import org.denigma.binding.extensions.sq
import org.denigma.binding.views.BindableView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.notebook.views.NotebookView
import org.scalajs.dom
import org.scalajs.dom.raw.Element

import scala.scalajs.js.annotation.JSExport

/**
 * Just a simple view for the whole app, if interested ( see https://github.com/antonkulaga/scala-js-binding )
 */
@JSExport("FrontEnd")
object FrontEnd extends BindableView with scalajs.js.JSApp
{

  override lazy val id: String = "body"

  lazy val elem: Element = dom.document.body


  /**
   * Register views
   */
  override lazy val injector = defaultInjector
    .register("Notebook"){case (el, args) =>
      val username = args.get("username").map(v=>v.toString).getOrElse("guest" + Math.random() * 1000)
      new NotebookView(el, username).withBinder(n => new CodeBinder(n))
    }

  this.withBinders(me => List(new GeneralBinder(me), new NavigationBinder(me)))

  @JSExport
  def main(): Unit = {
    this.bindView()
  }


  @JSExport
  def load(content: String, into: String): Unit = {
    dom.document.getElementById(into).innerHTML = content
  }

  @JSExport
  def moveInto(from: String, into: String): Unit = {
    for {
      ins <- sq.byId(from)
      intoElement <- sq.byId(into)
    } {
      this.loadElementInto(intoElement, ins.innerHTML)
      ins.parentNode.removeChild(ins)
    }
  }

}
