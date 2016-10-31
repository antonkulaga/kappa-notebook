package org.denigma.kappa.notebook.views.errors

import org.denigma.binding.binders.Events
import org.denigma.binding.views.{BindableView, CollectionSeqView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.messages.{Animate, Go, KappaMessage, KappaSourceFile}
import org.denigma.kappa.notebook.circuits.{ErrorsCircuit, KappaEditorCircuit}
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

/**
  * A view to show syntax erorrs
  * @param elem
  * @param circuit
  */
class SyntaxErrorsView(val elem: Element, circuit: ErrorsCircuit) extends CollectionSeqView {

  override type Item = (KappaSourceFile, WebSimError)
  override type ItemView =  WebSimErrorView

  val items: Rx[List[Item]] = circuit.filesWithErrors

  val hasErrors = items.map(f=>f.nonEmpty)

  override def newItemView(item: Item): ItemView = this.constructItemView(item){
    case (el, _)=>
      val errorCode = circuit.errorCode(item._2)
      //println(s"FROM $chFrom TO $chTo TEXT = $errorCode")
      new WebSimErrorView(el,  circuit.input, item._1, item._2, errorCode).withBinder(v=>new CodeBinder(v))
  }

}

class WebSimErrorView(val elem: Element, input: Var[KappaMessage], file: KappaSourceFile, error: WebSimError, textUnderError: String) extends BindableView {

  val tooltip = Var(textUnderError)

  val fileName = Var(file.name)
  val fromLine: Var[String] = Var[String](error.range.map(r=>(r.from_position.line + 1).toString).getOrElse(""))
  val fromChar = Var(error.range.map(r=>r.from_position.chr.toString).getOrElse(""))

  val toLine: Var[String] = Var(error.range.map(r=>(r.from_position.line + 1).toString).getOrElse(""))
  val toChar: Var[String] = Var(error.range.map(r=>r.from_position.chr.toString).getOrElse(""))

  val message = Var(error.message)
  val severity = Var(error.severity)
  val isError = Var(error.severity == "error")
  val isWarning = Var(error.severity == "warning")

  val goClick = Var(Events.createMouseEvent())
  goClick.triggerLater{
    input() = Animate(Go.ToFile(file), false)
  }


}
