package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionSeqView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaSourceFile
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.notebook.circuits.{ErrorsCircuit, KappaEditorCircuit}
import org.denigma.kappa.notebook.views.common.{FileTabHeaders, ServerConnections, TabHeaders}
import org.denigma.kappa.notebook.views.errors.SyntaxErrorsView
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.List
import scala.collection.immutable._



class KappaCodeEditor(val elem: Element,
                      val editorCircuit: KappaEditorCircuit,
                      val errorsCircuit: ErrorsCircuit
                     ) extends BindableView
  with CollectionSeqView
{

  val items = editorCircuit.openedFiles
  val selected: Var[String] = Var("")

  override protected def subscribeUpdates() = {
    template.hide()
    zipped.onChange{
      case (from, to) if from == to => //do nothing
      case (prev, cur) if prev !=cur =>
        val removed = prev.diff(cur)
        for(r <- removed) removeItemView(r)
        val added = cur.toSet.diff(prev.toSet)
        val revCur = cur.toList.reverse
        reDraw(revCur, added, template)
    }
    this.items.now.foreach(i => this.addItemView(i, this.newItemView(i)))
  }

  override type Item = Var[KappaSourceFile]

  override type ItemView = CodeTab

  //val isConnected = connections.map(c=>c.isConnected)

  val headers: Rx[List[String]] = items.map(its=>its.map(v=>v.now.path)) //TODO: check safety

  override def newItemView(item: Item): ItemView = this.constructItemView(item) {
    case (el, _) =>
      el.id = item.now.path //dirty trick
      val itemErrors = Var(List.empty[WebSimError])
      val view: ItemView = new CodeTab(el,
        item,
        selected,
        editorCircuit.input,
        editorCircuit.output,
        editorCircuit.editorsUpdates, editorCircuit.kappaCursor, itemErrors).withBinder(v => new CodeBinder(v)
      )
      selected() = item.now.path
      view
  }
  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new FileTabHeaders(el, headers, editorCircuit.input, selected)(TabHeaders.path2name).withBinder(new GeneralBinder(_)))
    .register("SyntaxErrors")((el, args) => new SyntaxErrorsView(el, errorsCircuit).withBinder(new GeneralBinder(_)))

}
