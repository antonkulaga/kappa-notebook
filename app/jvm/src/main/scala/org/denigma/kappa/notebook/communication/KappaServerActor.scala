package org.denigma.kappa.notebook.communication

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages.ServerMessages._
import org.denigma.kappa.messages.WebSimMessages._
import org.denigma.kappa.notebook.services.WebSimClient

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

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
    case RunAtServer(username, serverName, lm: LaunchModel, userRef, interval) if servers.contains(serverName)=>
      log.info(s"running model ${lm.runName} with files ${lm.fileNames} at server ${serverName}")

      val sink: Sink[server.flows.Runnable[server.flows.SimulationContactResult], Any] = Sink.foreach {
        case (Left( (token, res: SimulationStatus, connectionMap)), model) =>
          val result = SimulationResult(res, token, Some(model))
          userRef ! ServerResponse( serverName, result )

        case (Right(errors), model) =>
          val mess = SyntaxErrors(errors, model.files, onExecution = true)
          log.info(s"errors while running the model ${model.files.map(_._1)} at server ${serverName}:\n${errors}")
          userRef ! ServerResponse(serverName, mess )
      }
      val server = servers(serverName)
      server.runStreamed(lm, sink, interval)

    case RunAtServer(username, serverName, p: ParseModel, userRef, interval) if servers.contains(serverName)=>

      val sink: Sink[server.ContactMapResult, Any] = Sink.foreach {
        case Left( connectionMap) =>
          val mess = ParseResult(connectionMap)
          userRef ! ServerResponse(serverName, mess )

        case Right(errors) =>
          val mess = SyntaxErrors(errors, p.files, onExecution = false)
          //println("syntax errors "+mess)
          userRef ! ServerResponse(serverName, mess )
      }
      val server = servers(serverName)
      server.parse(ParseCode(p.code), sink)
  }

  protected def otherCases: PartialFunction[ServerMessage, Unit] = {
    case  launch @ RunAtServer(username, serverName, message, userRef, interval) =>
      system.log.error("DOES NOT EXIST: " + launch)
      userRef ! KappaServerErrors(List(s"Server $serverName does not respond"))

    case other => this.log.error(s"some other message $other")
  }

  protected def onServerCommands(sv: ServerMessage): Unit = runIfServerExists.orElse(otherCases)(sv)

  override def receive: Receive = {

    case ServerCommand(server, message) =>
      log.info(s"server command for $server")
      onServerCommands(message)

    case run: RunAtServer => onServerCommands(run)

    case other => this.log.error(s"some other message $other")
  }

}

case class RunAtServer(username: String, server: String, message: ServerMessage, userRef: ActorRef, interval: FiniteDuration) extends ServerMessage
