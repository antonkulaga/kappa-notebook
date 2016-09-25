package org.denigma.kappa.notebook.circuits

import org.denigma.kappa.messages.KappaMessage.ServerResponse
import org.denigma.kappa.messages.ServerMessages.{KappaServerErrors, SimulationResult, SyntaxErrors}
import org.denigma.kappa.messages.WebSimMessages.WebSimError
import org.denigma.kappa.messages._
import org.scalajs.dom
import rx.{Rx, Var}
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx.Dynamic

/**
  * Created by antonkulaga on 9/22/16.
  */
class ErrorsCircuit(input: Var[KappaMessage], output: Var[KappaMessage], runConfiguration: Rx[RunConfiguration]) extends Circuit(input, output){

  val serverErrors = Var(ServerErrors.empty)

  val kappaServerErrors = Var(KappaServerErrors.empty)

  val syntaxErrors = Var(SyntaxErrors.empty)

  val errorsByFiles: Rx[List[(String, WebSimError)]] = syntaxErrors.map(s=>s.errorsByFiles())

  val groupedErrors: Rx[Map[String, List[WebSimError]]] =  errorsByFiles.map(er=>er.groupBy{case (path, _) => path}.mapValues(l=>l.map{case (f, e)=>e}))

  val filesWithErrors = errorsByFiles.map{ ers=>
    val mp = runConfiguration.now.fileMap
    ers.collect{
      case (path, error) if{
        val exp = mp.contains(path)
        if(!exp) dom.console.error(s"error with $path was not found in run configuration, error is: ${error}")
        exp
      } =>
        mp(path) -> error
    }


  }
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
