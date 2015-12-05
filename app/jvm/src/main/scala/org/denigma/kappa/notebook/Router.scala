package org.denigma.kappa.notebook

import java.util.Date

import akka.actor.ActorSystem
import akka.http.extensions.security.LoginInfo
import akka.http.extensions.stubs.Registration
import akka.http.extensions.stubs._
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.stream.stage.{Context, PushStage, SyncDirective, TerminationDirective}
import org.denigma.kappa.notebook.pages._

import scala.concurrent.duration._

class Router(implicit fm: Materializer, system: ActorSystem) extends Directives {
  val sessionController:SessionController = new InMemorySessionController
  val loginController:InMemoryLoginController = new InMemoryLoginController()
  loginController.addUser(LoginInfo("admin","test2test","test@email"))

  def routes = new Head().routes ~
    new Registration(
      loginController.loginByName,
      loginController.loginByEmail,
      loginController.register,
      sessionController.userByToken,
      sessionController.makeToken
    )
      .routes ~
    new Pages().routes//~new WebSockets(SocketTransport(deviceActor).webSocketFlow).routes

  val theChat = Channel.create(system)
  import system.dispatcher
  system.scheduler.schedule(5.second, 5.second) {
    theChat.injectMessage(ChatMessage(sender = "clock", s"Bling! The time is ${new Date().toString}."))
  }

  def testBackFlow(channel:String,sender: String): Flow[String, Strict, Unit] =  Flow[String].collect{
    case message=>
      TextMessage.Strict(s"$sender: $message") // ... pack outgoing messages into WS text messages ...

  }

  def websocketChatFlow(channel:String,username: String): Flow[Message, Message, Unit] =
    Flow[Message]
      .collect {
      case TextMessage.Strict(msg) ⇒
        println(s"WE GOT $msg !")
        msg // unpack incoming WS text messages...
      // This will lose (ignore) messages not received in one chunk (which is
      // unlikely because chat messages are small) but absolutely possible
      // FIXME: We need to handle TextMessage.Streamed as well.
    }
      .via(testBackFlow(channel,username))
     /* .via(theChat.chatFlow(username)) // ... and route them through the chatFlow ...
      .map {
      case ChatMessage(sender, message) ⇒ TextMessage.Strict(s"$sender: $message") // ... pack outgoing messages into WS text messages ...
    }*/
      .via(reportErrorsFlow) // ... then log any processing errors on stdin

  def reportErrorsFlow[T]: Flow[T, T, Unit] =
    Flow[T]
      .transform(() ⇒ new PushStage[T, T] {
        def onPush(elem: T, ctx: Context[T]): SyncDirective = ctx.push(elem)

        override def onUpstreamFailure(cause: Throwable, ctx: Context[T]): TerminationDirective = {
          println(s"WS stream failed with $cause")
          super.onUpstreamFailure(cause, ctx)
        }
      })

}
