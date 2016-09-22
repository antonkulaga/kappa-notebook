package org.denigma.kappa.notebook.views.editor

import org.denigma.binding.binders.GeneralBinder
import org.denigma.binding.extensions._
import org.denigma.binding.views.{BindableView, CollectionSeqView}
import org.denigma.controls.code.CodeBinder
import org.denigma.kappa.messages.KappaSourceFile
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.notebook.circuits.KappaEditorCircuit
import org.denigma.kappa.notebook.views.common.TabHeaders
import org.denigma.kappa.notebook.views.errors.SyntaxErrorsView
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.List
import scala.collection.immutable._



class KappaCodeEditor(val elem: Element,
                      val circuit: KappaEditorCircuit
                     ) extends BindableView
  with CollectionSeqView
{

  val items = circuit.openedFiles
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

  //val fullCode = syntaxErrors.map(ers=>ers.fullCode)

/*
  val errorsInFiles: Rx[List[(KappaSourceFile, WebSimError)]] = syntaxErrors.map{ ers => ers.errorsByFiles().collect{
    case (filename, er) if {
      val exists = names.now.contains()
    } =>
      if(filename==""){
        val message = "error is out of bounds!"
        dom.console.error(message)
        dom.console.log("all errors "+ers.errors.mkString("\n"))
        dom.console.log("all filenames " + ers.files.map(kv=>kv._1).mkString(" | "))
      }
      if(!names.now.contains(filename)) dom.console.error(s"error refers to the $filename that was not found, message: ${er.message}")
      items.now.collectFirst{
        case i if i.name == filename => i -> er
      }
      //if(!items.now.exists(kv=>kv._2.name == filename)) dom.console.error(s"error refers to the $filename that was not found, message: ${er.message}")
      //items.now.collect{ case (str, file) if file.name == filename => file -> er }
    }
  }

  val errorsByFiles: Rx[Map[KappaSourceFile, List[WebSimError]]] = errorsInFiles.map{ byfiles => byfiles.groupBy{
        case (key, value) => key
      }.mapValues{ v => v.map(_._2) }
  }

  */
  val headers: Rx[SortedSet[String]] = itemViews.map{ its=>
    SortedSet.empty[String] ++ its.values.map(v => v.path.now)
  }

  override def newItemView(item: Item): ItemView = this.constructItemView(item) {
    case (el, _) =>
      el.id = item.now.path //dirty trick
      val itemErrors = Var(List.empty[WebSimError])
      val view: ItemView = new CodeTab(el, item,selected,  circuit.input, circuit.output, circuit.editorsUpdates, circuit.kappaCursor, itemErrors).withBinder(v => new CodeBinder(v) )
      selected() = item.now.path
      view
  }
  override lazy val injector = defaultInjector
    .register("headers")((el, args) => new TabHeaders(el, headers, selected)(TabHeaders.path2name).withBinder(new GeneralBinder(_)))
    .register("SyntaxErrors")((el, args) => new SyntaxErrorsView(el, circuit).withBinder(new GeneralBinder(_)))

}
