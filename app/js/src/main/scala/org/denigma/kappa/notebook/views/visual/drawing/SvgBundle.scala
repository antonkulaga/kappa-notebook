package org.denigma.kappa.notebook.views.visual.drawing

object SvgBundle {
  import scalatags.JsDom._
  import scalatags._

  object all extends Cap
    with jsdom.SvgTags
    with DataConverters
    with Aggregate
    with LowPriorityImplicits {
    object attrs extends Cap with SvgAttrs
  }
}