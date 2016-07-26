package org.denigma.kappa.notebook.styles

import scalacss.Defaults._

trait VisualizationStyles  extends StyleSheet.Standalone {
  import dsl._
  ".graph.container" -(
    minHeight(300 px),
    minWidth(300 px)
    )
  ".graph.container:before" -(
    content := "",
    display.block,
    paddingTop(100 %%)
    )
}
