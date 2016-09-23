package org.denigma.kappa.messages

import boopickle.DefaultBasic._
import boopickle.{CompositePickler, Pickler}
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, ServerConnection}
import org.denigma.kappa.messages.WebSimMessages.{RunModel, RunParameters, WebSimError}


object UIMessage {

  implicit val UIMessagePickler: CompositePickler[UIMessage] = compositePickler[UIMessage]
    //.addConcreteType[GoToPaper]
    //.addConcreteType[Go.ToSource]
    //.addConcreteType[Go.ToTab]
    .addConcreteType[MoveTo.Tab]
}
case object EmptyUIMEssage extends UIMessage


trait UIMessage extends KappaMessage


object MoveTo {
  object Tab {
    implicit val classPickler: Pickler[Tab]  = boopickle.Default.generatePickler[Tab]
  }
  case class Tab(name: String, shift: Int = 0, switch: Boolean = false) extends UIMessage //if shift
}


object SourceUpdate {
  //implicit val classPickler: Pickler[SourceUpdate] = boopickle.Default.generatePickler[SourceUpdate]
}
case class SourceUpdate(from: KappaSourceFile, to: KappaSourceFile) extends UIMessage

case class Errors(byFiles: Map[String, WebSimError], other: List[String]) extends UIMessage

object Animate {

  lazy val empty = Animate(EmptyKappaMessage, true)
}

case class Animate(message: KappaMessage, annotation: Boolean) extends UIMessage

case class RunConfiguration(files: List[KappaSourceFile],
                            parameters: RunParameters,
                            projectName: String,
                            configurationName: String = "default",
                            serverConnectionOpt: Option[ServerConnection] = None
                           ) extends UIMessage
{
  lazy val fullCode = files.foldLeft(""){case (acc, e) => acc + e}

  lazy val names = files.map(f=>f.name)

  lazy val pathes = files.map(f=>f.path)

}