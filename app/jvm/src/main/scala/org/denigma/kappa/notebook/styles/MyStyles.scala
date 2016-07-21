package org.denigma.kappa.notebook.styles

import org.denigma.controls.papers.MediaQueries

import scalacss.Defaults._

object MyStyles extends MainStyles
  with TextLayerStyles
  with TabGridsStyles
  with ListStyles
  with CodeStyles
{
  import dsl._

  ".fileitem" -{
    fontSize(0.9 em)
  }

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
}



