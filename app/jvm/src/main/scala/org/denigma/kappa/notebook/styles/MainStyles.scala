package org.denigma.kappa.notebook.styles


import org.denigma.controls.papers.MediaQueries

import scalacss.mutable.StyleSheet
import scalacss.Defaults._
import scalacss.mutable.StyleSheet.Standalone

/**
  * Created by antonkulaga on 7/17/16.
  */
class MainStyles extends Standalone with MediaQueries
{
  import dsl._

  lazy val rowMaxHeight = 95 vh
  lazy val gridMaxHeight = 89 vh

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
    onTiny   -width(380 vw),
    onLittle -width(375 vw),
    onSmall  -width(350 vw),
    onMedium -width(320 vw),
    onLarge  -width(290 vw)
    /*
    onTiny   -width(430 vw),
    onLittle -width(400 vw),
    onSmall  -width(380 vw),
    onMedium -width(340 vw),
    onLarge  -width(300 vw)
    */
    )

  "#main" -(
    overflowX.auto,
    overflowY.hidden
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
      minWidth(700 px)
    )
}
