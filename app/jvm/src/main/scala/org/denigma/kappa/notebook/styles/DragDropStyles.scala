package org.denigma.kappa.notebook.styles

import scala.language.postfixOps
import scalacss.Defaults._
/**
  * Created by antonkulaga on 09/06/16.
  */
trait DragDropStyles extends StyleSheet.Standalone {
  import dsl._

  ".dockable" -(
      borderWidth(4 px)  important,
      borderColor  :=! "green" important
    )
}
