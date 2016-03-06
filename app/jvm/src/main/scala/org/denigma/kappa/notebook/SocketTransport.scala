package org.denigma.kappa.notebook

import java.io.InputStream

import akka.NotUsed
import akka.http.scaladsl.model.ws.BinaryMessage.Strict
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.util.ByteString
import boopickle.Default._
import org.denigma.kappa.messages._
import org.denigma.kappa.syntax.Kappa
import fastparse.all._

/**
  * Websocket transport that unplickles/pickles messages
  */
class SocketTransport extends KappaPicklers {

  def webSocketFlow(channel: String, user: String = "guest"): Flow[Message, Message, _] = {
    openChannel(channel, user)
  }


  def reportErrorsFlow[T](channel: String, username: String): Flow[T, T, NotUsed] =
    Flow[T]
      .transform(() ⇒ new PushStage[T, T] {
        def onPush(elem: T, ctx: Context[T]): SyncDirective = ctx.push(elem)

        override def onUpstreamFailure(cause: Throwable, ctx: Context[T]): TerminationDirective = {
          println(s"WS stream for $channel failed for $username with the following cause:\n  $cause")
          super.onUpstreamFailure(cause, ctx)
        }
      })

  def readResource(path: String): Iterator[String] = {
    val stream : InputStream = getClass.getResourceAsStream(path)
    scala.io.Source.fromInputStream( stream ).getLines
  }

  def openChannel(channel: String, username: String = "guest"): Flow[Message, Strict, NotUsed] = (channel, username) match {
    case (_, _) =>
      Flow[Message].collect {
        case BinaryMessage.Strict(data) =>
          Unpickle[KappaMessages.Message].fromBytes(data.toByteBuffer) match {
            case cont: KappaMessages.Container =>
              val code = cont.code.head
              val params = cont.run.headOption.getOrElse(KappaMessages.RunParameters())
              val result = Kappa.run(code, params)
              val data = Pickle.intoBytes[KappaMessages.Message](result)
              BinaryMessage(ByteString(data))

            case KappaMessages.Load(modelName) =>
              val code = KappaMessages.Code(readResource("/examples/abc.ka").toList)
              val d = Pickle.intoBytes[KappaMessages.Message](code)
              BinaryMessage(ByteString(d))

            case code: KappaMessages.Code =>
              val params = KappaMessages.RunParameters()
              val result = Kappa.run(code, params)
              val data = Pickle.intoBytes[KappaMessages.Message](result)
              BinaryMessage(ByteString(data))
          }
      }.via(reportErrorsFlow(channel, username)) // ... then log any processing errors on stdin
  }

}
