package org.denigma.kappa.notebook

import akka.actor.ActorSystem
import akka.http.extensions.security.LoginInfo
import akka.http.extensions.stubs.{Registration, _}
import akka.http.scaladsl.server.Directives
import akka.stream.Materializer
import org.denigma.kappa.notebook.pages._

class Router(implicit fm: Materializer, system: ActorSystem) extends Directives {
  val sessionController: SessionController = new InMemorySessionController
  val loginController: InMemoryLoginController = new InMemoryLoginController()
  loginController.addUser(LoginInfo("admin", "test2test", "test@email"))

  val transport = new SocketTransport()

  def routes = new Head().routes ~
    new Registration(
      loginController.loginByName,
      loginController.loginByEmail,
      loginController.register,
      sessionController.userByToken,
      sessionController.makeToken
    )
      .routes ~
    new Pages().routes ~ new WebSockets(
    loginController.loginByName,
    loginController.loginByEmail,
    transport.openChannel).routes
}
