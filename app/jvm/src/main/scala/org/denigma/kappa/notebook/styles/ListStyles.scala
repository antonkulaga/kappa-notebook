package org.denigma.kappa.notebook.styles
import scalacss.Defaults._

trait ListStyles extends StyleSheet.Standalone {
  import dsl._

  ".clickable" -{
    cursor.pointer
  }

}
