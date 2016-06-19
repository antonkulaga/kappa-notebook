package org.denigma.kappa.messages

import boopickle.{CompositePickler, Pickler}
import boopickle.DefaultBasic._


object UIMessage {
  import boopickle.DefaultBasic._

  implicit val UIMessagePickler: CompositePickler[UIMessage] = compositePickler[UIMessage]
    //.addConcreteType[GoToPaper]
    .addConcreteType[Go.ToSource]
    .addConcreteType[Go.ToTab]
    .addConcreteType[MoveTo.Tab]
}


trait UIMessage extends KappaMessage

object Go {
  object ToTab {
    implicit val classPickler: Pickler[ToTab] = boopickle.Default.generatePickler[ToTab]
  }
  case class ToTab(name: String) extends UIMessage

  object ToSource {
    implicit val sourcePickler: Pickler[ToSource] = boopickle.Default.generatePickler[ToSource]
  }

  case class ToSource(filename: String, begin: Int = 0, end: Int = 0) extends UIMessage

}

object MoveTo {
  object Tab {
    implicit val classPickler: Pickler[Tab]  = boopickle.Default.generatePickler[Tab]
  }
  case class Tab(name: String, shift: Int = 0, switch: Boolean = false) extends UIMessage //if shift
}

