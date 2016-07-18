package org.denigma.kappa.notebook.styles
import scalacss.Defaults._
import scalacss.Defaults._
trait TabGridsStyles extends StyleSheet.Standalone with SimulationStyles {
  import dsl._

  ".ui.column" -(
    overflowY.auto,
    padding(0 px)  important
    )

  ".ui.number.input" -(
    maxWidth(40 px)
    )

}

trait SimulationStyles extends StyleSheet.Standalone {
  import dsl._

  ".simulation.tab" -(
    overflowX.visible,
    overflowY.visible
    )
}