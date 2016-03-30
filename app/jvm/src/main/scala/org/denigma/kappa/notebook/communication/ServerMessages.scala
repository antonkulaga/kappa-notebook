package org.denigma.kappa.notebook.communication

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.denigma.kappa.WebSim
import org.denigma.kappa.WebSim.{RunModel, SimulationStatus}
import org.denigma.kappa.notebook.services.WebSimClient

import scala.concurrent.duration.FiniteDuration

class KappaServerActor extends Actor with ActorLogging {

  ammonite.repl.Main.Config

  implicit def system: ActorSystem = context.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val server = new WebSimClient()(system, materializer)

  override def receive: Receive = {

    case ServerMessages.Run(username, serverName, message: WebSim.RunModel, userRef, interval) =>
      Source.single(message)
        .via(server.makeModelResultsFlow(1, interval))
        .runWith(Sink.foreach { case (token, res) =>  userRef ! ServerMessages.Result(serverName, res) })
    //server.runModelFlow

    //server.runWithStreaming(message, Sink.foreach{ case res =>  userRef ! ServerMessages.Result(serverName, res) }, interval)

    case other => this.log.error(s"some other message $other")
  }


}

object ServerMessages {
  trait ServerMessage
  {
    def server: String
  }

  //case class Run(username: String, server: String, message: WebSim.RunModel, userRef: ActorRef, interval: FiniteDuration) extends ServerMessage

  case class Run(username: String, server: String, message: WebSim.RunModel, userRef: ActorRef, interval: FiniteDuration) extends ServerMessage

  case class Result(server: String, simulationStatus: SimulationStatus) extends ServerMessage
}