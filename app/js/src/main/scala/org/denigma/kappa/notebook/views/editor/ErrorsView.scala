package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, CollectionSeqView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.messages.{KappaSourceFile, KappaMessage}
import org.denigma.kappa.notebook.actions.Movements
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

class ErrorsView(val elem: Element, input: Var[KappaMessage], val items: Rx[List[(KappaSourceFile, WebSimError)]], val fullCode: Rx[String]) extends CollectionSeqView {

  val hasErrors = items.map(f=>f.nonEmpty)

  override def newItemView(item: Item): ItemView = this.constructItemView(item){
    case (el, _)=>
      val code = fullCode.now
      val error = item._2
      val (chFrom ,chTo) = (error.range.from_position.chr, error.range.to_position.chr)
      val errorCode = code.substring(chFrom, chTo)
      println(s"FROM $chFrom TO $chTo TEXT = $errorCode")
      new WebSimErrorView(el, input, item._1, item._2, errorCode).withBinder(v=>new CodeBinder(v))
  }

  override type Item = (KappaSourceFile, WebSimError)
  override type ItemView =  WebSimErrorView
}

class WebSimErrorView(val elem: Element, input: Var[KappaMessage], file: KappaSourceFile, error: WebSimError, textUnderError: String) extends BindableView {

  val tooltip = Var(textUnderError)

  val fileName = Var(file.name)
  val fromLine = Var(error.range.from_position.line + 1)
  val fromChar = Var(error.range.from_position.chr)

  val toLine = Var(error.range.from_position.line + 1)
  val toChar = Var(error.range.from_position.chr)

  val message = Var(error.message)
  val severity = Var(error.severity)
  val isError = Var(error.severity == "error")
  val isWarning = Var(error.severity == "warning")

  val goClick = Var(Events.createMouseEvent())
  goClick.triggerLater{
    input() = Movements.toFile(file)
  }


}
