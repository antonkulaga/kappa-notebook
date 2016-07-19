package org.denigma.kappa.messages

import boopickle.CompositePickler

import scala.collection.immutable._
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.WebSimMessages._

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
    lazy val empty = SyntaxErrors(Nil, Nil)
  }

  case class SyntaxErrors(errors: List[WebSimError], files: List[(String, String)]) extends ServerMessage with FileContainer
  {
    def isEmpty = errors.isEmpty

    def errorsByFiles(): List[(String, WebSimError)] = {
      this.errors.map {
        case er =>
          (fileLocation(er.range.from_position), fileLocation(er.range.to_position))
          match {
            case (Some((f, from)), Some((_, to))) =>
              val newRange = er.range.copy(from_position = from, to_position = to)
              //println("ERROR WEBSIM FILE IS " + er.range.file)
              f -> er.copy(range = newRange)
            //er.copy(range = er.range.copy(from = from, to = to))
            case _ =>
              println("cannot find file for the range +" + er.range)
              "" -> er
          }
      }
    }
  }


  object SimulationResult {
    implicit val classPickler: Pickler[SimulationResult] = boopickle.Default.generatePickler[SimulationResult]
  }
  case class SimulationResult(simulationStatus: SimulationStatus, token: Int, initialParams: Option[LaunchModel] = None) extends ServerMessage

  object LaunchModel {
    implicit val classPickler: Pickler[LaunchModel] = boopickle.Default.generatePickler[LaunchModel]

    def fromRunModel(file: String, model: RunModel) = LaunchModel(List(file->model.code), max_events = model.max_events, max_time = model.max_time, nb_plot = model.nb_plot)
  }

  trait FileContainer {
    def files: List[(String, String)]

    lazy val fileLines: scala.List[(String, Array[String])] = files.map{
      case (key, value) => key -> value.split("\n", -1)
    }

    lazy val fullCode: String = files.foldLeft("") {
      case (acc, (name, content)) if content.endsWith("\n") => acc + content
      case (acc, (name, content)) => acc + content + "\n"
    }

    //https://github.com/antonkulaga/kappa-notebook/blob/master/websim/shared/src/main/scala/org/denigma/kappa/messages/WebSimMessages.scala

    lazy val fileSizes: scala.List[((Int, Int), String)] = fileLines.foldLeft(List.empty[((Int, Int), String)]) {
      case (Nil, (name, content)) => ((1, content.length), name) :: Nil

      case (
          (((prevFrom, prevTo)), prevName) :: tail, (name, content)
        ) =>
        val from = prevTo
        ((from, from + content.length), name) ::((prevFrom, prevTo), prevName) :: tail
    }.reverse

    def fileLocation(location: Location): Option[(String, Location)] = {
      val line = location.line
      fileSizes.collectFirst {
        case ((from, to), name) if line >= from && line <= to => name -> location.copy(line = line - from)
      }
    }
  }

  case class LaunchModel( files: List[(String, String)],
                         nb_plot: Option[Int] = Some(250),
                         max_events: Option[Int],
                         max_time: Option[Double] = None,
                         runName: String = "") extends ServerMessage with FileContainer
  {

    lazy val parameters = RunModel(fullCode, nb_plot, max_events, max_time)

  }

  object ParseModel {
    implicit val classPickler: Pickler[ParseModel] = boopickle.Default.generatePickler[ParseModel]
  }

  case class ParseModel(files: List[(String, String)]) extends ServerMessage
  {
    def lineToFileLine(num: Int): Option[(String, Int, String)] = zippedCode.find{
      case (_, i, _) => i == num
    }

    lazy val zippedCode: scala.List[(String, Int, String)] = files.foldLeft(List.empty[(String, Int, String)]){
      case (acc, (name, value))=>
        val start = if(acc.isEmpty) 1 else acc.head._2
        val arr: List[(String, Int, String)] = value.split("\n", -1).zipWithIndex.map{
          case (str, index) => (name , index + start , str)
        }.toList
        arr ++ acc
    }.reverse

    lazy val code = files.foldLeft(""){
      case (acc, (_, content))=> acc + content
    }
  }

  object ParseResult {
    implicit val classPickler: Pickler[ParseResult] = boopickle.Default.generatePickler[ParseResult]
  }
  case class ParseResult(contactMap: ContactMap) extends ServerMessage

}



