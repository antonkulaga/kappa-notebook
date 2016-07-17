package org.denigma.kappa.notebook.styles

import org.denigma.controls.papers.MediaQueries

import scalacss.mutable.StyleSheet

import scalacss.Defaults._
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

  /*
  ".page.content" - (
    height(100 %%),
    overflowY.auto
    )*/
}
