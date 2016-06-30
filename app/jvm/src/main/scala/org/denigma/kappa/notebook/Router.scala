package org.denigma.kappa.notebook

import java.io.{File => JFile}

import akka.actor.ActorSystem
import akka.http.extensions.security.LoginInfo
import akka.http.extensions.stubs.{Registration, _}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer
import better.files.File
import org.denigma.kappa.notebook.communication.WebSocketManager
import org.denigma.kappa.notebook.pages._

class Router(files: File)(implicit fm: Materializer, system: ActorSystem) extends Directives {

  implicit def ctx = system.dispatcher

  val sessionController: SessionController = new InMemorySessionController

  val loginController: InMemoryLoginController = new InMemoryLoginController()

  loginController.addUser(LoginInfo("admin", "test2test", "test@email"))

  val transport = new WebSocketManager(system, new FileManager(files))

  def loadFiles: Route = pathPrefix("files" ~ Slash) {
    getFromDirectory(files.path.toString)
  }

  lazy val registrationRoutes = new Registration(
    loginController.loginByName,
    loginController.loginByEmail,
    loginController.register,
    sessionController.userByToken,
    sessionController.makeToken
  ).routes

  lazy val websocketsRoutes = new WebSockets(
    //loginController.loginByName,
    //loginController.loginByEmail,
    transport.openChannel).routes

  lazy val headRoutes = new Head().routes

  lazy val pagesRoutes =  new Pages().routes

  def routes = headRoutes ~ loadFiles ~ registrationRoutes ~ websocketsRoutes ~ headRoutes ~ pagesRoutes
}
