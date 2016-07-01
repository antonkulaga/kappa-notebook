package org.denigma.kappa.notebook.styles

import scalacss.Defaults._

trait PanelStyles extends StyleSheet.Standalone{
  import dsl._
/*
  "#ProjectsPanel" -{
    maxWidth()
  }
  */
}


/**
  * Created by antonkulaga on 06/04/16.
  */
object MyStyles extends TextLayerStyles with DragDropStyles with TabGridsStyles with ListStyles
{
  import dsl._

  val totalWidth = 300 vw //5120 px//5632.0 px

  ".selectable" -{
    cursor.pointer
  }

  ".fileitem" -{
    fontSize(0.9 em)
  }


  ".project.content" -{
    cursor.pointer
  }

  "html" - (
    height(100 %%),
    overflowX.auto
  )

  "body" - (
    height(100 %%),
    overflowX.auto
    )

  "#Notebook" -(
    maxHeight(95 vh),
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
    borderWidth(3 px)
    )

  ".pages.row" - {
    maxHeight(98 vh)
    }



  ".CodeMirror" -(
    height.auto important,
    minHeight(15.0 vh),
    maxHeight(100 %%),
    width(100 %%)
    //height(100.0 %%) important
    // width.auto important
    )

  ".CodeMirror-scroll" -(
    overflow.visible,
    height.auto
    )//-(overflowX.auto,overflowY.hidden)

  ".breakpoints" - (
    width( 3 em)
    )

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



