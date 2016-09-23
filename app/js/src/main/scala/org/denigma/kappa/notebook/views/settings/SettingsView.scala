package org.denigma.kappa.notebook.views.settings

import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.actions.Commands
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.circuits.SettingsCircuit
import org.denigma.kappa.notebook.graph.layouts.{ForceLayoutParams, LayoutMode}
import org.denigma.threejs.Vector3

import scala.concurrent.duration._


object AnnotationMode extends Enumeration {
  type AnnotationMode = Value
  val ToEditor, ToAnnotation = Value
}

class SettingsView(val elem: Element, circuit: SettingsCircuit) extends BindableView {

  val toAnnotation = Var(true)

  def annotationMode = circuit.annotationMode

  toAnnotation.foreach{ v => if(v) annotationMode() = AnnotationMode.ToAnnotation }

  val toEditor = Var(false)
  toEditor.foreach{v => if(v) annotationMode() = AnnotationMode.ToEditor}

  val websim = Var("http://localhost:8080")

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
    circuit.input() = Commands.SaveAll
    scalajs.js.timers.setTimeout(autosaveDuraction)(sendSaveCommand())
  }

  val rulesGraphRepulsion = Var(100)
  val rulesGraphAttraction = Var(0.9)
  val rulesGraphGravity = Var(1)
  val rulesMode = Var(LayoutMode.TwoD)
  val rulesCenter = Var(new Vector3(0, 0, 0))

  val rulesGraphParams = Rx{
    val repulsion = rulesGraphRepulsion()
    val spring = rulesGraphAttraction()
    val gravity = rulesGraphGravity()
    val mode = rulesMode()
    val center = rulesCenter()
    ForceLayoutParams(repulsion, spring, gravity, center, mode)
  }

  rulesGraphParams.foreach{ params =>
    println("send params = ")
    pprint.pprintln(params)
    circuit.output() = Commands.SetLayoutParameters("rules", params)
  }

  val fluxGraphRepulsion = Var(30)
  val fluxGraphAttraction = Var(10)
  val fluxGraphGravity = Var(1)
  val fluxMode = Var(LayoutMode.ThreeD)
  val fluxCenter = Var(new Vector3(0, 0 ,0))

  val fluxGraphParams = Rx{
    val repulsion = fluxGraphRepulsion()
    val spring = fluxGraphAttraction()
    val gravity = fluxGraphGravity()
    val mode = fluxMode()
    val center = fluxCenter()
    ForceLayoutParams(repulsion, spring, gravity, center, mode)
  }

  fluxGraphParams.foreach{ params =>
    circuit.output() = Commands.SetLayoutParameters("flux", params)
  }

}
