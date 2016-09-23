package org.denigma.kappa.notebook.circuits

import org.denigma.kappa.messages.{KappaMessage, UIMessage}
import org.denigma.kappa.notebook.views.common.ServerConnections
import org.denigma.kappa.notebook.views.settings.AnnotationMode
import rx.Var

import rx.Ctx.Owner.Unsafe.Unsafe
/**
  * Created by antonkulaga on 9/14/16.
  */
class SettingsCircuit(input: Var[UIMessage], output: Var[UIMessage]) extends Circuit(input, output){

  lazy val annotationMode = Var(AnnotationMode.ToAnnotation)

  val websimConnections: Var[ServerConnections] = Var(ServerConnections.default)

  lazy val currentServer = websimConnections.map(wc=>wc.currentServer)

  override protected def onInputMessage(message: UIMessage): Unit = message match {


    case _=> //do nothing
  }
}
