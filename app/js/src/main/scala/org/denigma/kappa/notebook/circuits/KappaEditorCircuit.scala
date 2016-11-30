package org.denigma.kappa.notebook.circuits

import org.denigma.binding.extensions._
import org.denigma.kappa.messages.KappaMessage.ServerCommand
import org.denigma.kappa.messages.ServerMessages.ParseModel
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.actions.Commands.CloseFile
import org.denigma.kappa.notebook.views.comments.CommentsWatcher
import org.denigma.kappa.notebook.views.editor.{EditorUpdates, EmptyCursor, KappaCursor}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._
import scala.concurrent.duration._
import scala.scalajs.js
/**
  * Created by antonkulaga on 9/15/16.
  */
class KappaEditorCircuit(input: Var[KappaMessage],
                         output: Var[KappaMessage],
                         val runConfiguration: Rx[RunConfiguration]
                        ) extends Circuit(input, output){

  val openOrder: Var[List[String]] = Var(Nil)

  val items = Var(Map.empty[String, KappaSourceFile])
  items.updates.onChange(updateOpened)

  protected def updateOpened(upd: MapUpdate[String, KappaSourceFile]) = {
    openOrder() = openOrder.now.filterNot(o=>upd.removed.contains(o)) ++ upd.added.keysIterator.toList
  }

  val kappaCursor: Var[KappaCursor] = Var(EmptyCursor)

  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  val commentManager = new CommentsWatcher(editorsUpdates, input)

  val currentServer = runConfiguration.map(c=>c.serverConnectionOpt.map(cc=>cc.server).getOrElse(""))

  protected def onInputMessage(message: KappaMessage) = message match {

    case ProjectResponses.LoadedProject(proj) =>
      val files = proj.sourceMap.collectFirst{
        case (path, f) if f.name.toLowerCase.contains("readme") =>
          Map( (path, f))
      }
      .getOrElse(Map.empty[String, KappaSourceFile])
      items() = files
      js.timers.setTimeout(300 millis){
        //check for syntax errors after loading
        val toParse =  ParseModel(runConfiguration.now.tuples)
        output() = ServerCommand(currentServer.now, toParse)
      }

    /*
    case Commands.OpenFile(f: KappaSourceFile)  if !hasFile(f.path)=>
      openedFiles() = openedFiles.now :+ Var(f)
    */
    case Go.ToFile(f: KappaSourceFile)  if !items.now.contains(f.path)=>
      println(s"openning KappaSourceFile ${f.path}")
      items() = items.now.updated(f.path, f)

    case CloseFile(path) if items.now.contains(path)=>
      items() = items.now - path

    case other => //do nothing
  }

}