package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.{Events, GeneralBinder}
import org.denigma.binding.views.{BindableView, ItemsSeqView}
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.messages.{KappaFile, KappaMessage}
import org.denigma.kappa.notebook.views.actions.Movements
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

/**
  * Created by antonkulaga on 18/07/16.
  */
class ErrorsView(val elem: Element, input: Var[KappaMessage], errorsByFile: Rx[Map[KappaFile, List[WebSimError]]]) extends ItemsSeqView {

  //val hasErrors = syntaxErrors.map(er => !er.isEmpty)
  //override val items: Rx[Seq[Item]] = syntaxErrors.map(er=>er.errors)

  val hasErrors = errorsByFile.map(f=>f.nonEmpty)
  val items: Rx[List[(KappaFile, WebSimError)]] = errorsByFile.map{
    case mp => mp.toList.flatMap{ case (key, ers) => ers.map(e=>key->e)}
  }

  override def newItemView(item: Item): ItemView = this.constructItemView(item){
    case (el, _)=> new WebSimErrorView(el, input, item._1, item._2).withBinder(v=>new GeneralBinder(v))
  }

  override type Item = (KappaFile, WebSimError)
  override type ItemView =  WebSimErrorView
}

class WebSimErrorView(val elem: Element, input: Var[KappaMessage], file: KappaFile, error: WebSimError) extends BindableView {

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
