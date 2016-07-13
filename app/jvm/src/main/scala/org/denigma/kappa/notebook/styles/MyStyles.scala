package org.denigma.kappa.notebook.styles

import org.denigma.controls.papers.MediaQueries

import scalacss.Defaults._

trait PanelStyles extends StyleSheet.Standalone{
  import dsl._

  "#ProjectsPanel" -{
    minWidth(310 px)
  }
}



trait CodeStyles extends StyleSheet.Standalone with MediaQueries{
  import dsl._

  ".CodeMirror" -(
    height.auto important,
    minHeight(15.0 vh),
    maxHeight(100 %%),
    width(100 %%),
    &("pre") -(
      onTiny -(fontSize(8 pt) important),
      onLittle -(fontSize(9 pt) important),
      onSmall -(fontSize(10 pt) important),
      onMedium -(fontSize(11 pt) important),
      onLarge -(fontSize(12 pt) important)
      )
    //height(100.0 %%) important
    // width.auto important
    )

  ".CodeMirror-scroll" -(
    overflow.visible,
    height.auto
    )//-(overflowX.auto,overflowY.hidden)

  ".breakpoints" - (
    width( 1 em)
    )

  ".pointed" -{
    cursor.pointer
  }
}

/**
  * Created by antonkulaga on 06/04/16.
  */
object MyStyles extends TextLayerStyles
  with DragDropStyles
  with TabGridsStyles
  with ListStyles
  with MediaQueries
  with CodeStyles
  with PanelStyles
{
  import dsl._

  val totalWidth = 300 vw //5120 px//5632.0 px

  "html"-(
    onTiny -fontSize(8 pt),
    onLittle -fontSize(9 pt),
    onSmall -fontSize(10 pt),
    onMedium -fontSize(11 pt),
    onLarge -fontSize(12 pt)
    )

  ".selectable" -{
    cursor.pointer
  }

  ".fileitem" -{
    fontSize(0.9 em)
  }


  ".project.content" -{
    cursor.pointer
  }

  "#Notebook" -(
    maxHeight(90 vh),
    minWidth(totalWidth)
    )

  "#main" -(
    overflowX.auto,
    overflowY.hidden,
    maxHeight(98 vh)
    )

  "#grid" -(
    overflowX.auto,
    overflowY.hidden,
    maxHeight(98 vh)
    )

  ".graph" -(
    borderColor(blue),
    borderWidth(4 px)
    )

  ".pages.row" - {
    maxHeight(96 vh)
    }

  ".focused" - (
    backgroundColor.ghostwhite
    )

  "#Papers" -(
    padding(0 px)
    )

  ".ui.segment.paper" -(
    padding(0 px),
    minHeight(98.0 vh)
    )


  "#LeftGraph" -(
    padding(0 px)
    )

  "#RightGraph" -(
    padding(0 px)
    )

  ".project.content" -{
    cursor.pointer
  }
}



