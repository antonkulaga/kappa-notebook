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

  protected def names = runConfiguration.now.names


  lazy val errorsInFiles: Rx[List[(KappaSourceFile, WebSimError)]] = syntaxErrors.map{ ers => ers.errorsByFiles().collect{
    case (filename, er) if {
      val exists = names.contains(filename)
      exists
    } =>
      if(filename==""){
        val message = "error is out of bounds!"
        dom.console.error(message)
        dom.console.log("all errors "+ers.errors.mkString("\n"))
        dom.console.log("all filenames " + ers.files.map(kv=>kv._1).mkString(" | "))
      }
      if(!names.contains(filename)) dom.console.error(s"error refers to the $filename that was not found, message: ${er.message}")
      runConfiguration.now.files.collectFirst{
        case i if i.name == filename => i -> er
      }.get
    //if(!items.now.exists(kv=>kv._2.name == filename)) dom.console.error(s"error refers to the $filename that was not found, message: ${er.message}")
    //items.now.collect{ case (str, file) if file.name == filename => file -> er }
    }
  }

  def errorCode(error: WebSimError): String = {
    val code = runConfiguration.now.fullCode
    val (chFrom ,chTo) = (error.range.from_position.chr, error.range.to_position.chr)
    code.substring(chFrom, chTo)
  }


  override protected def onInputMessage(message: KappaMessage): Unit = message match {

    case KappaMessage.ServerResponse(server, ers: ServerErrors) =>  serverErrors() = ers

    case KappaMessage.ServerResponse(server, ers: KappaServerErrors) => kappaServerErrors() = ers

    case Failed(operation, ers, username) =>  kappaServerErrors() = kappaServerErrors.now.copy(errors = kappaServerErrors.now.errors ++ ers)

    case KappaMessage.ServerResponse(server, SimulationResult(status, token, params) ) =>
      kappaServerErrors() = KappaServerErrors.empty

    case s: ServerErrors =>
      dom.console.error("server errors = "+s.errors)
      serverErrors() = s



    case _ => //do nothing
  }
}
