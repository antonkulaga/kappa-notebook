package org.denigma.kappa.notebook.templates

import scalacss.Defaults._

object MyStyles extends StyleSheet.Standalone {
  import dsl._

  ".errors" -(
    borderColor.red
    )

 /* "#container" - (
    width(1024 px),
    height(768 px)
    )
*/
}