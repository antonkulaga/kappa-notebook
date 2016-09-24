package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.Events
import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.codemirror._
import org.denigma.codemirror.addons.lint._
import org.denigma.codemirror.extensions._
import org.denigma.kappa.messages._
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.notebook.views.common.TabItem
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx.Ctx.Owner.Unsafe.Unsafe

import scalajs.js.JSConverters._
import rx._

import scala.concurrent.duration._
import scala.scalajs.js

class RDFCodeTab(elem: Element,
                   source: Var[KappaSourceFile],
                   selected: Var[String],
                   val input: Var[KappaMessage],
                   val output: Var[KappaMessage],
                   editorUpdates: Var[EditorUpdates],
                   kappaCursor: Var[KappaCursor],
                   val errors: Rx[List[WebSimError]]
                  ) extends CodeTab(elem, source, selected, editorUpdates: Var[EditorUpdates], kappaCursor: Var[KappaCursor])
{
  override def mode = "text/turtle"

}
