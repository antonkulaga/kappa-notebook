package org.denigma.kappa.notebook.styles
import scalacss.Defaults._
trait TabGridsStyles extends MainStyles {
  import dsl._

  ".ui.column" -(
    overflowY.auto,
    padding(0 px)  important
    )

  ".graph.row" -(
      padding(0 px) important
    )

  ".graph.column" -(
    padding(0 px) important,
    margin(0 px) important,
    borderWidth(2 px) important,
    borderColor := "deepskyblue"
    )

  ".ui.page.grid" -(
    paddingLeft(5 px) important,
    paddingRight(5 px) important
    )

  ".paper.grid" -(
    minHeight(80.0 vh) important
    )

  ".page.tab" - (
    overflowY.hidden important,
    overflowX.hidden important
    )

  "wide tab segment" - (
    width(100 %%)
    )

  "rect.caption" - (
    svgStrokeWidth := "0px"
    )

  "page-container" -(
    padding(0 px),
    overflowX.visible
    )

  ".page" -(
    overflowY.hidden important,
    overflowX.hidden important,
    //overflowX.scroll,
    minHeight(80.0 vh) important
    //height(100.0 %%)
    )

  ".subpage" -(
    /*
    onTiny -(height(65 vh) important),
    onLittle -(height(66 vh) important),
    onSmall -(height(68 vh) important),
    onMedium -(height(70 vh) important),
    onLarge -(height(75 vh) important)
    */
    height(70 vh) important,
    overflow.visible important
    //overflowY.scroll
    )

  ".smallpage" -(
    overflowY.auto,
    onTiny -(maxHeight(50 vh) important),
    onLittle -(maxHeight(53 vh) important),
    onSmall -(maxHeight(55 vh) important),
    onMedium -(maxHeight(58 vh) important),
    onLarge -(maxHeight(60 vh) important)
    )

  ".flexible.page" -(
    overflowY.auto,
    overflowX.auto,
    height(100.0 %%)
    )

  ".simulation" -(
    overflowX.visible,
    overflowY.visible
    )

  "#ProjectsPanel" -(
    minWidth(345 px) important
    )


  ".paper.grid" -(
    maxHeight(78 vh) important,
    overflowY.auto important
    )

  ".ui.segment.paper" -(
    overflowY.auto,
    maxHeight(62 vh)
    )

}