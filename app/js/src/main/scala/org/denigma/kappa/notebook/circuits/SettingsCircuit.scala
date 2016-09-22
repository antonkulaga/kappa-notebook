package org.denigma.kappa.notebook.circuits

import org.denigma.kappa.messages.KappaMessage
import org.denigma.kappa.notebook.views.common.ServerConnections
import org.denigma.kappa.notebook.views.settings.AnnotationMode
import rx.Var

/**
  * Created by antonkulaga on 9/14/16.
  */
class SettingsCircuit(input: Var[KappaMessage], output: Var[KappaMessage]) extends Circuit(input, output){

  lazy val annotationMode = Var(AnnotationMode.ToAnnotation)
  val serverConfiguration: Var[ServerConnections] = Var(ServerConnections.default)

  override protected def onInputMessage(message: KappaMessage): Unit = message match {
    case _=> //do nothing
  }
}
