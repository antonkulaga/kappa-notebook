package org.denigma.kappa.notebook.pages

import scalacss.Defaults._


/**
  * Created by antonkulaga on 06/04/16.
  */
object MyStyles extends TextLayerStyles{
  import dsl._

  "#Notebook" -(
    maxHeight(95 vh),
    minWidth(2500.0 px)
    )

  "#Scroller" -(
    minWidth(2500.0 px)
    )

  "#main" -(
    overflowX.auto,
    overflowY.hidden,
    maxHeight(98 vh)
    )
  ".graph" -(
    borderColor(blue),
    borderWidth(3 px)
    )

  ".attached.tab.segment" -(
      overflowY.auto
    )

  ".ui.column" -(
    overflowY.auto  important,
    padding(0 px)  important
    )


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
    width( 2 em)
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

  ".tab.page" -(
      //overflowY.scroll,
      //overflowX.scroll,
      minHeight(98.0 vh)
    )

  ".tab.flexible.page" -(
    overflowY.auto,
    overflowX.auto,
    height(100.0 %%)
    )

  "#LeftGraph" -(
    padding(0 px)
    )

  "#RightGraph" -(
    padding(0 px)
    )
}

trait TextLayerStyles extends StyleSheet.Standalone {
  import dsl._
  ".textLayer" -(

    position.absolute,
    left(0 px),
    top( 0 px),
    right (0 px),
    bottom (0 px),
    overflow.hidden,
    opacity(0.2),
    lineHeight(1.0),
    transformOrigin := "0% 0%"
    )

   ".textLayer > div" -(
     color.transparent,
     position.absolute,
     whiteSpace.pre,
     cursor.text,
     transformOrigin := "0% 0%"
     )

  ".textLayer .highlight" - (
    margin(-1.0 px),
    padding(1 px),
    backgroundColor.rgb(180, 0, 170),
    borderRadius( 4 px)
    )
  ".textLayer .highlight.begin" -(
    borderRadius(4 px, 0 px , 0 px , 4 px)
    )

  ".textLayer .highlight.end" -(
    borderRadius(0 px, 4 px , 4 px , 0 px)
    )

  ".textLayer .highlight.middle" -(
    borderRadius(0 px)
    )

  ".textLayer .highlight.middle" -(
    borderRadius(0 px)
    )

  ".textLayer .highlight.selected" -(
    backgroundColor(rgb(0, 100, 0))
    )

  ".textLayer ::selection" -(
    backgroundColor(rgb(0, 0, 255))
    )

   ".textLayer ::-moz-selection " -(
     backgroundColor(rgb(0, 0, 255))
     )

   ".textLayer .endOfContent" -(
     display.block,
     position.absolute,
     left(0 px),
     top(100 %%),
     right(0 px),
     bottom(0 px),
     zIndex(-1),
     cursor.default,
     userSelect := "none"

     )

    ".textLayer .endOfContent.active" -(
      top(0 px)
      )
}
