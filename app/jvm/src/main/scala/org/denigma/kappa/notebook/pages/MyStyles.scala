package org.denigma.kappa.notebook.pages

import scalacss.Defaults._
import scalacss.Percentage

/**
  * Created by antonkulaga on 06/04/16.
  */
object MyStyles extends StyleSheet.Standalone {
  import dsl._

  ".CodeMirror" -(
    height.auto important
    // width.auto important
    )
  ".CodeMirror-scroll" -(
    overflow.visible,
    height.auto
    )//-(overflowX.auto,overflowY.hidden)

  ".breakpoints" - (
    width( 2 em)
    )

  ".focused" - (
    backgroundColor.ghostwhite
    )

  "#Papers" -(
    padding(0 px)
    )

  "#the-canvas" -(

    )

  "#Tabs" -(
    overflowY.scroll,
    overflowX.scroll,
    height(100.0 %%)
    )
}
