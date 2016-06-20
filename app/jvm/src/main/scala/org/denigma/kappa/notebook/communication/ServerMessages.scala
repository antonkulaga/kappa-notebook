package org.denigma.kappa.notebook.communication

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages.ServerMessages._
import org.denigma.kappa.messages.WebSimMessages._
import org.denigma.kappa.notebook.services.WebSimClient
import rx.Var

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Either

class KappaServerActor extends Actor with ActorLogging {

  implicit def system: ActorSystem = context.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import net.ceedubs.ficus.Ficus._
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._

  import com.typesafe.config.Config
  val config: Config = system.settings.config
  val defaultServers = config.as[List[ServerConnection]]("app.servers")

  var servers= defaultServers.map{
    case s => s.name -> new WebSimClient(s)(system, materializer)
  }.toMap


  protected def addServer(s: WebSimClient) = {
    val version: Future[Version] = s.getVersion()
    import akka.pattern._
    import akka.pattern.pipe
    //version.pipeTo(self)
  }


  protected def runIfServerExists: PartialFunction[ServerMessage, Unit] ={
    case launch @ RunAtServer(username, serverName, message: RunModel, userRef, interval) if servers.contains(serverName)=>
      println("run model")

      val sink: Sink[server.flows.Runnable[server.flows.SimulationContactResult], Any] = Sink.foreach {
        case (Left( (token, res: SimulationStatus, con)), model) =>

          val mess = SimulationResult(serverName, res, token, Some(model))
          //log.info("result is:\n "+mess)

          userRef ! ServerResponse( SimulationResult(serverName, res, token, Some(model)) )

        case (Right(errors), model) =>
          val mess = SyntaxErrors(serverName, errors, Some(model))
          //log.info("result is with errors "+mess)
          userRef ! ServerResponse( mess )
      }
      val server = servers(serverName)
      server.runStreamed(message, sink, interval)
  }

  protected def otherCases: PartialFunction[ServerMessage, Unit] = {
    case  launch @ RunAtServer(username, serverName, message: RunModel, userRef, interval) =>
      system.log.error("DOES NOT EXIST: " + launch)
      userRef ! KappaServerErrors(List(s"Server $serverName does not respond"))

    case other => this.log.error(s"some other message $other")
  }

  protected def onServerCommands(sv: ServerMessage): Unit = runIfServerExists.orElse(otherCases)(sv)


  override def receive: Receive = {

    case ServerCommand(message) => onServerCommands(message)

    case run: RunAtServer => onServerCommands(run)

    case other => this.log.error(s"some other message $other")
  }


}

case class RunAtServer(username: String, server: String, message: RunModel, userRef: ActorRef, interval: FiniteDuration) extends ServerMessage
