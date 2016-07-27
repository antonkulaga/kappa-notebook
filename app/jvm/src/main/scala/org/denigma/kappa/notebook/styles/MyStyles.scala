package org.denigma.kappa.notebook.styles

import org.denigma.controls.papers.MediaQueries

import scalacss.Defaults._

object MyStyles extends MainStyles
  with TextLayerStyles
  with TabGridsStyles
  with ListStyles
  with CodeStyles
  with VisualizationStyles
{
  import dsl._

  ".fileitem" -{
    fontSize(0.9 em)
  }

  ".plot" -(
      maxWidth(600 px),
      maxHeight(60 vh)
    )

  ".noscroll" -(
    overflowX.hidden important,
    overflowY.hidden important
    )

  "#runner" -(
    width(100 %%)
    )

  ".stack" -(
    display.inlineFlex,
    flexDirection.row,
    flexWrap.nowrap,
    alignContent.stretch
    )

  ".ui.table td.collapsing" -(
    padding(0 px) important
    )

  ".ui.segment.paper" -(
    overflowY.auto,
    maxHeight(58 vh)
    )
}



