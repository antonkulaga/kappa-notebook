package org.denigma.kappa.notebook.communication

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages.ServerMessages._
import org.denigma.kappa.messages.WebSimMessages._
import org.denigma.kappa.notebook.services.WebSimClient

import scala.concurrent.duration.FiniteDuration

/**
  * An actor that is used to run WebSim server
  */
class KappaServerActor extends Actor with ActorLogging {

  implicit def system: ActorSystem = context.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import com.typesafe.config.Config
  import net.ceedubs.ficus.Ficus._
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._
  val config: Config = system.settings.config
  val defaultServers = config.as[List[ServerConnection]]("app.servers")

  var servers= defaultServers.map{ s => s.name -> new WebSimClient(s)(system, materializer) }.toMap

  /**
    * Processes requests that should execute code on the server
    * @return
    */
  protected def runIfServerExists: PartialFunction[ServerMessage, Unit] ={

    //starts simulation on the server and streams the results to the sender
    case RunAtServer(username, serverName, lm: LaunchModel, userRef, interval) if servers.contains(serverName)=>
      val sink: Sink[server.flows.Runnable[server.flows.SimulationContactResult], Any] = Sink.foreach {
        case (Left( (token, res: SimulationStatus, connectionMap)), model) =>
          val result = SimulationResult(res, token, Some(model))
          val resp = ServerResponse( serverName, result )
          userRef ! resp

        case (Right(errors), model) =>
          val mess = SyntaxErrors(errors, model.files, onExecution = true)
          //log.info(s"errors while running the model ${model.files.map(_._1)} at server ${serverName}:\n${errors}")
          userRef ! ServerResponse(serverName, mess )
      }
      val server = servers(serverName)
      server.runStreamed(lm, sink, interval)

    //checks kappa model for errors and returns either ContactMap or syntax errors
    case RunAtServer(username, serverName, p: ParseModel, userRef, interval) if servers.contains(serverName)=>

      val sink: Sink[server.ContactMapResult, Any] = Sink.foreach {
        case Left( connectionMap) =>
          val mess = ParseResult(connectionMap)
          userRef ! ServerResponse(serverName, mess )

        case Right(errors) =>
          val mess = SyntaxErrors(errors, p.files, onExecution = false)
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

  /**
    * Main receive methods that process messages that go to the actor
    * @return
    */
  override def receive: Receive = {

    case ServerCommand(server, message) =>
      log.info(s"server command for $server")
      onServerCommands(message)

    case run: RunAtServer => onServerCommands(run)

    case other => this.log.error(s"some other message $other")
  }

}

case class RunAtServer(username: String, server: String, message: ServerMessage, userRef: ActorRef, interval: FiniteDuration) extends ServerMessage
