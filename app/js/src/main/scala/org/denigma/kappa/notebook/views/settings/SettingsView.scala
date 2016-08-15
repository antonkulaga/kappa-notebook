package org.denigma.kappa.notebook.views.settings

import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.actions.Commands
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import scala.concurrent.duration._


object AnnotationMode extends Enumeration {
  type AnnotationMode = Value
  val ToEditor, ToAnnotation = Value
}

class SettingsView(val elem: Element, val input: Var[KappaMessage]) extends BindableView {
  val toAnnotation = Var(true)
  toAnnotation.onChange{ v => if(v) annotationMode() = AnnotationMode.ToAnnotation }

  val toEditor = Var(false)
  toEditor.onChange{v => if(v) annotationMode() = AnnotationMode.ToEditor}

  val annotationMode: Var[AnnotationMode.AnnotationMode] = Var(AnnotationMode.ToAnnotation)
  annotationMode.onChange //UGFLY PART TODO: fix it
  {
    case AnnotationMode.ToAnnotation =>
      toAnnotation.Internal.value = true
      toEditor.Internal.value = false

    case AnnotationMode.ToEditor =>
      toAnnotation.Internal.value = false
      toEditor.Internal.value = true
  }


  val autosave = Var(true)
  val autosaveDuraction = 10 seconds


  override def bindView()
  {
    super.bindView()
    autosave.foreach{
      case true => sendSaveCommand()
      case false =>
    }
  }

  protected def sendSaveCommand(): Unit  = if(autosave.now) {
    input() = Commands.SaveAll
    scalajs.js.timers.setTimeout(autosaveDuraction)(sendSaveCommand())
  }


}
