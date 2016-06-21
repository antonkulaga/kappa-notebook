package org.denigma.kappa.messages

import boopickle.CompositePickler

import scala.collection.immutable.{List, Nil}
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.WebSimMessages.{ContactMap, RunModel, SimulationStatus, WebSimError}

object ServerMessages {

  object ServerMessage {
    implicit val classPickler: CompositePickler[ServerMessage] = compositePickler[ServerMessage]
      .addConcreteType[KappaServerErrors]
      .addConcreteType[SyntaxErrors]
      .addConcreteType[Connect]
      .addConcreteType[SimulationResult]
      .addConcreteType[ParseResult]
      .addConcreteType[ParseModel]
      .addConcreteType[LaunchModel]
      .addConcreteType[ConnectedServers]
      .addConcreteType[ServerConnection]
  }

  trait ServerMessage

  object ServerConnection {
    implicit val classPickler: Pickler[ServerConnection] = boopickle.Default.generatePickler[ServerConnection]

    lazy val default: ServerConnection = new ServerConnection("localhost", "localhost", 8080)
  }

  case class ServerConnection(name: String, host: String, port: Int) extends ServerMessage {
    def server = name
  }

  object ConnectedServers {
    lazy val empty: ConnectedServers = ConnectedServers(Nil)
    implicit val classPickler: Pickler[ConnectedServers] = boopickle.Default.generatePickler[ConnectedServers]
  }

  case class ConnectedServers(servers: List[ServerConnection]) extends ServerMessage

  object KappaServerErrors {
    implicit val classPickler: Pickler[KappaServerErrors] = boopickle.Default.generatePickler[KappaServerErrors]
    lazy val empty = KappaServerErrors(Nil)
  }
  case class KappaServerErrors(errors: List[String])  extends ServerMessage

  object Connect
  {
    implicit val classPickler: Pickler[Connect] = boopickle.Default.generatePickler[Connect]
  }
  case class Connect(server: ServerConnection) extends ServerMessage

  object SyntaxErrors {
    implicit val classPickler: Pickler[SyntaxErrors] = boopickle.Default.generatePickler[SyntaxErrors]
    lazy val empty = SyntaxErrors("", Nil)
  }

  case class SyntaxErrors(server: String, errors: List[WebSimError], initialParams: Option[RunModel] = None) extends ServerMessage
  {
    def isEmpty = errors.isEmpty
  }

  object SimulationResult {
    implicit val classPickler: Pickler[SimulationResult] = boopickle.Default.generatePickler[SimulationResult]
  }
  case class SimulationResult(server: String, simulationStatus: SimulationStatus, token: Int, initialParams: Option[RunModel] = None) extends ServerMessage

  object LaunchModel {
    implicit val classPickler: Pickler[LaunchModel] = boopickle.Default.generatePickler[LaunchModel]
  }
  case class LaunchModel(server: String, parameters: RunModel, counter: Int = 0) extends ServerMessage

  object ParseModel {
    implicit val classPickler: Pickler[ParseModel] = boopickle.Default.generatePickler[ParseModel]
  }

  case class ParseModel(server: String, files: List[(String, String)]) extends ServerMessage
  {
    def lineToFileLine(num: Int): Option[(String, Int, String)] = zippedCode.find{
      case (_, i, _) => i == num
    }

    lazy val zippedCode: scala.List[(String, Int, String)] = files.foldLeft(List.empty[(String, Int, String)]){
      case (acc, (name, value))=>
        val start = if(acc.isEmpty) 1 else acc.head._2
        val arr: List[(String, Int, String)] = value.split("\n").zipWithIndex.map{
          case (str, index) => (name , index + start , str)
        }.toList
        arr ++ acc
    }.reverse

    lazy val code = files.foldLeft(""){
      case (acc, el)=> acc + el
    }
  }

  object ParseResult {
    implicit val classPickler: Pickler[ParseResult] = boopickle.Default.generatePickler[ParseResult]
  }
  case class ParseResult(server: String, contactMap: ContactMap) extends ServerMessage

}



