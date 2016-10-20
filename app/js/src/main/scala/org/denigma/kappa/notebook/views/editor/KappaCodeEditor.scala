package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.views.CollectionMapView
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaSourceFile
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.notebook.circuits.{ErrorsCircuit, KappaEditorCircuit}
import org.denigma.kappa.notebook.views.common.{FileTabHeaders, TabHeaders}
import org.denigma.kappa.notebook.views.errors.SyntaxErrorsView
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.collection.immutable._



class KappaCodeEditor(val elem: Element,
                      val editorCircuit: KappaEditorCircuit,
                      val errorsCircuit: ErrorsCircuit
                     ) extends CollectionMapView
{

  type Key = String
  type Value = KappaSourceFile
  val headers = editorCircuit.openOrder
  val items = editorCircuit.items
  val isEmpty = items.map(its=>its.isEmpty)

  val selected: Var[String] = Var("")

  override type ItemView = KappaCodeTab

  override def onRemove(item: Item):Unit = {
    val sel = selected.now
    super.onRemove(item)
    if(sel == item && items.now.nonEmpty) {
      selected() = headers.now.last
    }
  }

  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new FileTabHeaders(el, headers, editorCircuit.input, selected)(TabHeaders.path2name).withBinder(new GeneralBinder(_)))
    .register("SyntaxErrors")((el, args) => new SyntaxErrorsView(el, errorsCircuit).withBinder(new GeneralBinder(_)))

  override def updateView(view: KappaCodeTab, key: String, old: KappaSourceFile, current: KappaSourceFile): Unit = {
    view.source() = current
  }

  override def newItemView(key: String, value: KappaSourceFile): KappaCodeTab = this.constructItemView(key) {
    case (el, _) =>
      val itemErrors =  errorsCircuit.groupedErrors.map(gp => gp.getOrElse(key, List.empty[WebSimError]))
      val view: ItemView = new KappaCodeTab(el,
        Var(value),
        selected,
        editorCircuit.input,
        editorCircuit.output,
        editorCircuit.editorsUpdates,
        editorCircuit.kappaCursor,
        itemErrors).withBinder(v => new CodeBinder(v)
      )
      selected() = key
      view
  }
}
