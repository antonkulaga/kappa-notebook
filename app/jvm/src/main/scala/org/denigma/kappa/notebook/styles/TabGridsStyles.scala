package org.denigma.kappa.notebook.styles
import scalacss.Defaults._
import scalacss.Defaults._
trait TabGridsStyles extends StyleSheet.Standalone {
  import dsl._

  ".attached.tab.segment" -(
    overflowY.auto
    )

  ".ui.column" -(
    overflowY.auto  important,
    padding(0 px)  important
    )

  ".ui.number.input" -(
    maxWidth(50 px)
    )


  ".tab.page" -(
    //overflowY.scroll,
    //overflowX.scroll,
    minHeight(90.0 vh),
    height(100.0 %%)
    )

  ".tab.flexible.page" -(
    overflowY.auto,
    overflowX.auto,
    height(100.0 %%)
    )

}
