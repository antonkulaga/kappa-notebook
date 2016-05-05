package org.denigma.kappa.notebook.communication

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.denigma.kappa.messages.{RunModel, SimulationStatus, _}
import org.denigma.kappa.notebook.services.WebSimClient

import scala.concurrent.duration.FiniteDuration
import scala.util.Either

class KappaServerActor extends Actor with ActorLogging {

  ammonite.repl.Main.Config

  implicit def system: ActorSystem = context.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val server = new WebSimClient()(system, materializer)
    override def receive: Receive = {

    case launch @ RunAtServer(username, serverName, message: RunModel, userRef, interval) =>
      Source.single(message)
      val sink: Sink[(Either[(Int, SimulationStatus), List[String]], RunModel), Any] = Sink.foreach {
        case (Left( (token, res: SimulationStatus)), model) =>

          val mess = SimulationResult(serverName, res, token, Some(model))
          log.info("result is:\n "+mess)

          userRef ! SimulationResult(serverName, res, token, Some(model))

        case (Right(errors), model) =>
          val mess = SyntaxErrors(serverName, errors, Some(model))
          log.info("result is with errors "+mess)
          userRef ! mess
      }
      server.runStreamed(message, sink, interval)
        //.via(server.makeModelResultsFlow(1, interval))
        //.runWith(Sink.foreach { case (token, res) =>  userRef ! ServerMessages.Result(serverName, res) })
    //server.runModelFlow

    //server.runWithStreaming(message, Sink.foreach{ case res =>  userRef ! ServerMessages.Result(serverName, res) }, interval)

    case other => this.log.error(s"some other message $other")
  }


}

case class RunAtServer(username: String, server: String, message: RunModel, userRef: ActorRef, interval: FiniteDuration) extends ServerMessage
