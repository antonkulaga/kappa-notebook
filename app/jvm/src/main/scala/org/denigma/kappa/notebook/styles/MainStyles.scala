package org.denigma.kappa.notebook.styles

import org.denigma.controls.papers.MediaQueries

import scalacss.Defaults._
import scalacss.internal.mutable.StyleSheet.Standalone

/**
  * Created by antonkulaga on 7/17/16.
  */
class MainStyles extends Standalone with MediaQueries
{
  import dsl._

  lazy val rowMaxHeight = 96 vh
  lazy val gridMaxHeight = 90 vh

  "html"-(
    onTiny -fontSize(8 pt),
    onLittle -fontSize(9 pt),
    onSmall -fontSize(10 pt),
    onMedium -fontSize(11 pt),
    onLarge -fontSize(12 pt)
    )

  "body" -(
    //overflowY.hidden
    )

  "#MainRow" - (
    maxHeight(rowMaxHeight),
    paddingTop(10 px),
    paddingBottom(5 px)
    )

  "#MainGrid" -(
    maxHeight(gridMaxHeight),
    onTiny   -width(375 vw),
    onLittle -width(350 vw),
    onSmall  -width(325 vw),
    onMedium -width(300 vw),
    onLarge  -width(275 vw)
    )

  "#main" -(
    //.overflowX.auto,
    //overflowY.hidden
    )

  "#grid" -(
    overflowX.auto,
    overflowY.hidden
    )

  ".project.content" -{
    cursor.pointer
  }

  ".pointed" - cursor.pointer

  ".dockable" -(
    borderWidth(5 px)  important,
    borderColor  :=! "green" important,
    borderStyle.dashed
    )

  ".attached.tab.segment" - overflowY.auto

  ".selectable" -{
    cursor.pointer
  }


  ".content.row" - (
    height(gridMaxHeight),
    overflowX.auto,
    overflowY.auto
    )

  ".scrollable" -(
      overflowX.auto,
      overflowY.auto
    )

  "#VisualPanel" - (
      minWidth(600 px)
    )
}
