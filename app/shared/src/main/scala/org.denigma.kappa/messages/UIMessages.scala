package org.denigma.kappa.messages


object Go {
  object ToTab {
    import boopickle.DefaultBasic._
    implicit val classPickler = boopickle.Default.generatePickler[ToTab]
  }
  case class ToTab(name: String) extends UIMessage

  object ToSource {
    import boopickle.DefaultBasic._
    implicit val sourcePickler = boopickle.Default.generatePickler[ToSource]
  }

  case class ToSource(filename: String, begin: Int = 0, end: Int = 0) extends UIMessage

}

object MoveTo {
  object Tab {
    import boopickle.DefaultBasic._
    implicit val classPickler = boopickle.Default.generatePickler[Tab]
  }
  case class Tab(name: String, shift: Int = 0, switch: Boolean = false) extends UIMessage //if shift
}

