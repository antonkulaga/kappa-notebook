package org.denigma.kappa.notebook.circuits

import org.denigma.kappa.messages.KappaMessage.ServerResponse
import org.denigma.kappa.messages.ServerMessages.{KappaServerErrors, SimulationResult, SyntaxErrors}
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.messages._
import org.scalajs.dom
import rx.{Rx, Var}
import rx.Ctx.Owner.Unsafe.Unsafe

/**
  * Created by antonkulaga on 9/22/16.
  */
class ErrorsCircuit(input: Var[KappaMessage], output: Var[KappaMessage], runConfiguration: Rx[RunConfiguration]) extends Circuit(input, output){

  val serverErrors = Var(ServerErrors.empty)

  val kappaServerErrors = Var(KappaServerErrors.empty)

  val syntaxErrors = Var(SyntaxErrors.empty)

  protected def pathes= runConfiguration.now.pathes


  val errorsInFiles: Rx[List[(KappaSourceFile, WebSimError)]] = {
    syntaxErrors.map{ ers => ers.errorsByFiles().collect{
      case (filepath, er) if {
        println(s"SYNTAX ERROR: ${filepath}")
        val exists = pathes.contains(filepath)
        exists
      } =>
        if(filepath==""){
          val message = "error is out of bounds!"
          dom.console.error(message)
          dom.console.log("all errors "+ers.errors.mkString("\n"))
          dom.console.log("all filenames " + ers.files.map(kv=>kv._1).mkString(" | "))
        }
        if(!pathes.contains(filepath)) dom.console.error(s"error refers to the $filepath that was not found, message: ${er.message}")
        val fl: (KappaSourceFile, WebSimError) = runConfiguration.now.files.collectFirst{
          case i if i.path == filepath => i -> er
        }.get
        fl
      //if(!items.now.exists(kv=>kv._2.name == filename)) dom.console.error(s"error refers to the $filename that was not found, message: ${er.message}")
      //items.now.collect{ case (str, file) if file.name == filename => file -> er }
      }
    }
  }

  val groupedErrors =  errorsInFiles.map(ers=>ers.groupBy(kv=>kv._1).mapValues(v=>v.map(_._2)))

  def errorCode(error: WebSimError): String = {
    val code = runConfiguration.now.fullCode
    val (chFrom ,chTo) = (error.range.from_position.chr, error.range.to_position.chr)
    code.substring(chFrom, chTo)
  }


  override protected def onInputMessage(message: KappaMessage): Unit = message match {

    case KappaMessage.ServerResponse(server, ServerMessages.ParseResult(cmap))=>
      syntaxErrors() = SyntaxErrors.empty

    case KappaMessage.ServerResponse(server, ers: SyntaxErrors) =>
      syntaxErrors() = ers

    case ers: SyntaxErrors =>
      syntaxErrors() = ers

    case Failed(operation, ers, username) =>
      kappaServerErrors() = kappaServerErrors.now.copy(errors = kappaServerErrors.now.errors ++ ers)

    case KappaMessage.ServerResponse(server, ers: KappaServerErrors) =>
      kappaServerErrors() = ers

    case KappaMessage.ServerResponse(server, SimulationResult(status, token, params) ) =>
      kappaServerErrors() = KappaServerErrors.empty

    case KappaMessage.ServerResponse(server, ers: ServerErrors) =>
      serverErrors() = ers

    case s: ServerErrors =>
      //dom.console.error("server errors = "+s.errors)
      serverErrors() = s


    case _ => //do nothing
  }
}
