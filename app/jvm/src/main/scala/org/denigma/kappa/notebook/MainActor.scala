package org.denigma.kappa.notebook

import akka.actor._
import akka.http.scaladsl.Http.{ServerBinding, IncomingConnection}
import akka.http.scaladsl.{Http, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.Future

/**
 * Main actor that encapsulates main application logic and starts the server
 */
class MainActor  extends Actor with ActorLogging
{

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val server: HttpExt = Http(context.system)
  var serverSource: Source[IncomingConnection, Future[ServerBinding]] = null
  val router = new Router()

  override def receive: Receive = {
    case AppMessages.Start(config)=>
      val (host,port) = (config.getString("app.host") , config.getInt("app.port"))
      log.info(s"starting server at $host:$port")
      server.bindAndHandle(router.routes, host, port)

    case AppMessages.Stop=> onStop()
  }

  def onStop() = {
    log.info("Main actor has been stoped...")
  }

  override def postStop() = {
    onStop()
  }


}
