package org.denigma.kappa.notebook.circuits

import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages.ServerMessages.{ParseModel, SyntaxErrors}
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.actions.Commands.CloseFile
import org.denigma.kappa.notebook.views.comments.CommentsWatcher
import org.denigma.kappa.notebook.views.common.ServerConnections
import org.denigma.kappa.notebook.views.editor.{EditorUpdates, EmptyCursor, KappaCursor}
import org.scalajs.dom
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

/**
  * Created by antonkulaga on 9/15/16.
  */
class KappaEditorCircuit(input: Var[KappaMessage], output: Var[KappaMessage],
                         val runConfiguration: Rx[RunConfiguration]
                        ) extends Circuit(input, output){

  val openedFiles: Var[List[Var[KappaSourceFile]]] = Var(Nil)

  val kappaCursor: Var[KappaCursor] = Var(EmptyCursor)

  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  val commentManager = new CommentsWatcher(editorsUpdates, input)

  val currentServer = runConfiguration.map(c=>c.serverConnectionOpt.map(cc=>cc.server).getOrElse(""))

  protected def hasFile(path: String) = openedFiles.now.exists(f=>f.now.path == path)

  protected def onInputMessage(message: KappaMessage) = message match {


    case Go.ToFile(f: KappaSourceFile)  if !hasFile(f.path)=>
      openedFiles() = openedFiles.now :+ Var(f)

    case CloseFile(path) if hasFile(path) =>
      openedFiles() = openedFiles.now.filterNot(f=>f.now.path == path)


    case SourceUpdate(from, to) =>

        output() = ServerCommand(currentServer.now, ParseModel(runConfiguration.now.files.map(f=>f.name->f.content)))

    case other => //do nothing
  }

  //def childCircuit(ed: EditorUpdates)


}