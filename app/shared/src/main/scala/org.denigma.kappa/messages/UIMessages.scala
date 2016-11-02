package org.denigma.kappa.messages

import boopickle.DefaultBasic._
import boopickle.{CompositePickler, Pickler}
import org.denigma.kappa.messages.ServerMessages.{LaunchModel, ServerConnection}
import org.denigma.kappa.messages.WebSimMessages.{RunParameters, WebSimError}


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


case class Errors(byFiles: Map[String, WebSimError], other: List[String]) extends UIMessage

object Animate {

  lazy val empty = Animate(EmptyKappaMessage, annotation = true)
}

case class Animate(message: KappaMessage, annotation: Boolean) extends UIMessage

case class RunConfiguration(files: List[KappaSourceFile],
                            parameters: RunParameters,
                            projectName: String,
                            configurationName: String = "default",
                            serverConnectionOpt: Option[ServerConnection] = None
                           ) extends UIMessage
{

  lazy val fileMap = files.map(f=>f.path->f).toMap

  lazy val fullCode = files.foldLeft(""){case (acc, e) => acc + (if(e.content.endsWith("\n")) e.content else e.content + "\n")}

  lazy val names = files.map(f=>f.name)

  lazy val pathes = files.map(f=>f.path)

  lazy val tuples = files.map(f=>f.path -> f.content)

  lazy val launchModel: LaunchModel = LaunchModel(tuples, parameters.plot_period, parameters.max_events, parameters.max_time, configurationName, runCount = parameters.runCount)

}