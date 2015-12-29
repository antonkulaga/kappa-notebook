package org.denigma.kappa.notebook

import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.util.ByteString
import boopickle.Default._
import org.denigma.kappa.messages.KappaMessages.Console
import org.denigma.kappa.messages._
import org.denigma.kappa.syntax.{Kappa, KappaResult}

import ammonite.ops
import ammonite.ops._


class SocketTransport extends KappaPicklers {

  def webSocketFlow(channel: String, user: String = "guest"): Flow[Message, Message, _] = {
    openChannel(channel, user)
  }


  def reportErrorsFlow[T](channel: String, username: String): Flow[T, T, Unit] =
    Flow[T]
      .transform(() ⇒ new PushStage[T, T] {
        def onPush(elem: T, ctx: Context[T]): SyncDirective = ctx.push(elem)

        override def onUpstreamFailure(cause: Throwable, ctx: Context[T]): TerminationDirective = {
          println(s"WS stream for $channel failed for $username with the following cause:\n  $cause")
          super.onUpstreamFailure(cause, ctx)
        }
      })


  def openChannel(channel: String, username: String = "guest"): Flow[Message, Message, Unit] = (channel, username) match {
    case (_, _) =>
      Flow[Message].collect {
        case BinaryMessage.Strict(data) =>
          Unpickle[KappaMessages.Message].fromBytes(data.toByteBuffer) match {

            case cont: KappaMessages.Container =>
              val KappaResult(command, lines) = Kappa.run(cont.code.head, cont.run.headOption.getOrElse(KappaMessages.RunParameters()))
              val series = command.out.lines
              val disc: Console = KappaMessages.Console(series)
              val d = Pickle.intoBytes[KappaMessages.Message](disc)
              println(s"RESULTS WITH CONTAINER: \n ${disc.lines.mkString("\n")}")
              BinaryMessage(ByteString(d))

            case KappaMessages.Load(modelName) =>
              val res: String = read.resource ! Path("examples") / modelName
              val code = KappaMessages.Code(res)
              println("CODE TO SEND "+res)
              val d = Pickle.intoBytes[KappaMessages.Message](code)
              BinaryMessage(ByteString(d))

            case code: KappaMessages.Code =>
              println(s"code received $code")
              val KappaResult(command, lines) = Kappa.run(code, KappaMessages.RunParameters())
              val series = command.out.lines
              val disc: Console = KappaMessages.Console(series)
              val d = Pickle.intoBytes[KappaMessages.Message](disc)
              println(s"RESULTS: \n ${disc.lines.mkString("\n")}")
              BinaryMessage(ByteString(d))
          }
      }.via(reportErrorsFlow(channel, username)) // ... then log any processing errors on stdin
  }

}
