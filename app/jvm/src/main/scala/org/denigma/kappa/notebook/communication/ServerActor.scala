package org.denigma.kappa.notebook.communication

import akka.actor.{ActorRef, ActorLogging, Actor}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.denigma.kappa.WebSim
import org.denigma.kappa.WebSim.{SimulationStatus, RunModel}
import org.denigma.kappa.notebook.services.WebSimClient
import scala.concurrent.duration.FiniteDuration

class ServerActor extends Actor with ActorLogging {

  implicit def system = context.system
  implicit val materializer = ActorMaterializer()

  val server = new WebSimClient()(system, materializer)

  override def receive: Receive = {

    case ServerMessages.Run(username, serverName, message: WebSim.RunModel, userRef, interval) =>

      server.runWithStreaming(message, Sink.foreach{ case res =>
        println(s"next chunk: ${res.percentage}/n data is ${res}")
        userRef ! ServerMessages.Result(serverName, res)
      }, interval)

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