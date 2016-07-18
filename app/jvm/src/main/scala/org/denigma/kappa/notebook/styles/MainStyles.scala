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

  lazy val rowMaxHeight = 98 vh
  lazy val gridMaxHeight = 90 vh

  "html"-(
    onTiny -fontSize(8 pt),
    onLittle -fontSize(9 pt),
    onSmall -fontSize(10 pt),
    onMedium -fontSize(11 pt),
    onLarge -fontSize(12 pt)
    )

  "#MainRow" - maxHeight(rowMaxHeight)

  "#MainGrid" -(
    maxHeight(gridMaxHeight),
    onTiny   -width(380 vw),
    onLittle -width(375 vw),
    onSmall  -width(350 vw),
    onMedium -width(320 vw),
    onLarge  -width(290 vw)
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

  ".page.tab" - (
    overflowY.hidden important,
    overflowX.hidden important
    )

  ".dockable" -(
    borderWidth(5 px)  important,
    borderColor  :=! "green" important,
    borderStyle.dashed
    )

  ".attached.tab.segment" - overflowY.auto

  ".selectable" -{
    cursor.pointer
  }


  ".focused" - backgroundColor.ghostwhite


  ".content.row" - (
    height(gridMaxHeight),
    overflowX.auto,
    overflowY.auto
    )

  ".tab.page" -(
    overflowY.hidden important,
    overflowX.hidden important,
    //overflowX.scroll,
    minHeight(80.0 vh) important
    //height(100.0 %%)
    )

  ".tab.flexible.page" -(
    overflowY.auto,
    overflowX.auto,
    height(100.0 %%)
    )

}
