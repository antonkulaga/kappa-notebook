package org.denigma.kappa.notebook.styles

import org.denigma.controls.papers.MediaQueries

import scalacss.mutable.StyleSheet

import scalacss.Defaults._
trait CodeStyles extends MainStyles{

  import dsl._

  val maxCodeHeight = 70 vh

  ".CodeMirror" -(
    overflowX.auto,
    //overflowY.auto,
    //overflow.visible important,
    height.auto important,
    //height(maxCodeHeight),
    //height.auto important,
    //minHeight(15.0 vh),
    maxHeight(maxCodeHeight) important,
    //width(100 %%) important,
    &("pre") -(
      onTiny -(fontSize(8 pt) important),
      onLittle -(fontSize(9 pt) important),
      onSmall -(fontSize(9 pt) important),
      onMedium -(fontSize(10 pt) important),
      onLarge -(fontSize(10 pt) important)
      )
    )

  ".CodeMirror-linenumber" -(
    onTiny -(fontSize(8 pt) important),
    onLittle -(fontSize(9 pt) important),
    onSmall -(fontSize(9 pt) important),
    onMedium -(fontSize(10 pt) important),
    onLarge -(fontSize(10 pt) important)
    )


  ".CodeMirror-scroll" -(
    maxHeight(maxCodeHeight) important
      /*
    onTiny -(fontSize(72 pt) important),
    onLittle -(fontSize(9 pt) important),
    onSmall -(fontSize(9 pt) important),
    onMedium -(fontSize(10 pt) important),
    onLarge -(fontSize(10 pt) important)
    //maxHeight(85 vh)
    */
    )//-(overflowX.auto,overflowY.hidden)



  ".breakpoints" - (
    width( 1 em)
    )


  ".focused" - backgroundColor.lightskyblue

  /*
  ".page.content" - (
    height(100 %%),
    overflowY.auto
    )*/
}
