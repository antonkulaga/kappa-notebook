package org.denigma.kappa.messages

import boopickle.DefaultBasic._
import boopickle.{CompositePickler, Pickler}
import org.denigma.kappa.messages.WebSimMessages.WebSimError


object UIMessage {

  implicit val UIMessagePickler: CompositePickler[UIMessage] = compositePickler[UIMessage]
    //.addConcreteType[GoToPaper]
    //.addConcreteType[Go.ToSource]
    //.addConcreteType[Go.ToTab]
    .addConcreteType[MoveTo.Tab]
}


trait UIMessage extends KappaMessage


object MoveTo {
  object Tab {
    implicit val classPickler: Pickler[Tab]  = boopickle.Default.generatePickler[Tab]
  }
  case class Tab(name: String, shift: Int = 0, switch: Boolean = false) extends UIMessage //if shift
}


object SourceUpdate {
  implicit val classPickler: Pickler[SourceUpdate] = boopickle.Default.generatePickler[SourceUpdate]
}
case class SourceUpdate(from: KappaSourceFile, to: KappaSourceFile) extends UIMessage

case class Errors(byFiles: Map[String, WebSimError], other: List[String]) extends UIMessage

object Animate {

  lazy val empty = Animate(EmptyKappaMessage, true)
}

case class Animate(message: KappaMessage, annotation: Boolean) extends UIMessage