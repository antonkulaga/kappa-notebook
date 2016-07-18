package org.denigma.kappa.notebook.styles

import org.denigma.controls.papers.MediaQueries

import scalacss.mutable.StyleSheet

import scalacss.Defaults._
trait CodeStyles extends MainStyles{

  import dsl._

  ".CodeMirror" -(
    height.auto important,
    minHeight(15.0 vh),
    maxHeight(gridMaxHeight),
    width(100 %%) important,
    &("pre") -(
      onTiny -(fontSize(8 pt) important),
      onLittle -(fontSize(9 pt) important),
      onSmall -(fontSize(9 pt) important),
      onMedium -(fontSize(10 pt) important),
      onLarge -(fontSize(10 pt) important)
      )
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
