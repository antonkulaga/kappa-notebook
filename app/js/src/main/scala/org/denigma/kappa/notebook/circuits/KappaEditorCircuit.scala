package org.denigma.kappa.notebook.circuits

import org.denigma.kappa.messages.KappaMessage.ServerResponse
import org.denigma.kappa.messages.ServerMessages.SyntaxErrors
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.actions.Commands.CloseFile
import org.denigma.kappa.notebook.views.comments.CommentsWatcher
import org.denigma.kappa.notebook.views.editor.{EditorUpdates, EmptyCursor, KappaCursor}
import org.scalajs.dom
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

/**
  * Created by antonkulaga on 9/15/16.
  */
class KappaEditorCircuit(input: Var[KappaMessage], output: Var[KappaMessage],
                         val runnableFiles: Rx[List[KappaSourceFile]]
                        ) extends Circuit(input, output){

  val openedFiles: Var[List[Var[KappaSourceFile]]] = Var(Nil)

  val names = runnableFiles.map(fls=>fls.map(f=>f.name))

  val kappaCursor: Var[KappaCursor] = Var(EmptyCursor)

  val editorsUpdates: Var[EditorUpdates] = Var(EditorUpdates.empty)

  val commentManager = new CommentsWatcher(editorsUpdates, input)

  val syntaxErrors = Var(SyntaxErrors.empty)

  lazy val fullCode = runnableFiles.map(fls => fls.foldLeft(""){case (acc, e) => acc + e})

  lazy val errorsInFiles: Rx[List[(KappaSourceFile, WebSimError)]] = syntaxErrors.map{ ers => ers.errorsByFiles().collect{
    case (filename, er) if {
      val exists = names.now.contains(filename)
      exists
    } =>
      if(filename==""){
        val message = "error is out of bounds!"
        dom.console.error(message)
        dom.console.log("all errors "+ers.errors.mkString("\n"))
        dom.console.log("all filenames " + ers.files.map(kv=>kv._1).mkString(" | "))
      }
      if(!names.now.contains(filename)) dom.console.error(s"error refers to the $filename that was not found, message: ${er.message}")
      runnableFiles.now.collectFirst{
        case i if i.name == filename => i -> er
      }.get
    //if(!items.now.exists(kv=>kv._2.name == filename)) dom.console.error(s"error refers to the $filename that was not found, message: ${er.message}")
    //items.now.collect{ case (str, file) if file.name == filename => file -> er }
    }
  }

  protected def hasFile(path: String) = openedFiles.now.exists(f=>f.now.path == path)

  protected def onInputMessage(message: KappaMessage) = message match {

    case KappaMessage.ServerResponse(server, ServerMessages.ParseResult(cmap)) => //hide syntax errors when parsing succedded
      syntaxErrors() = SyntaxErrors.empty

    case ServerResponse(server, s: SyntaxErrors) =>
      dom.console.log("syntax errors = "+s)
      syntaxErrors() = s

    case Go.ToFile(f: KappaSourceFile)  if !hasFile(f.path)=>
      openedFiles() = openedFiles.now :+ Var(f)

    case CloseFile(path) if hasFile(path) =>
      openedFiles() = openedFiles.now.filterNot(f=>f.now.path == path)


    case SourceUpdate(from, to) =>
     //   output() = ServerCommand(connections.now.currentServer, ParseModel(files))

    case other => //do nothing
  }

  //def childCircuit(ed: EditorUpdates)


}