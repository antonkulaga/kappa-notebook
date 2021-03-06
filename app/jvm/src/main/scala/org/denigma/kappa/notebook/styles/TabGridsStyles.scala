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
    onTiny -( minHeight(47 vh) important, maxHeight(57 vh) important),
    onLittle -( minHeight(49 vh) important, maxHeight(59 vh) important),
    onSmall -( minHeight(51 vh) important, maxHeight(60 vh) important),
    onMedium -( minHeight(53 vh)important, maxHeight(62 vh) important),
    onLarge -( minHeight(55 vh) important, maxHeight(64 vh) important),
    overflowY.hidden important
    //overflowY.auto important
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
    //overflowY.hidden important,
    overflowX.hidden important,
    onTiny -( minHeight(65 vh) important),
    onLittle -( minHeight(67 vh) important),
    onSmall -( minHeight(69 vh) important),
    onMedium -( minHeight(71 vh)important),
    onLarge -( minHeight(73 vh) important)
    //overflowX.scroll,
    //minHeight(80.0 vh) important
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
    onTiny -( minHeight(63 vh) important),
    onLittle -( minHeight(65 vh) important),
    onSmall -( minHeight(67 vh) important),
    onMedium -( minHeight(69 vh)important),
    onLarge -( minHeight(71 vh) important),
    overflow.visible important
    //overflowY.scroll
    )

  ".dense.column" -(
    padding(0 px) important
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

  ".ui.segment.paper" -(
    overflowY.auto,
    maxHeight(62 vh)
    )

  "#ProjectsPanel" -(
    minWidth(345 px) important,
    overflowY.auto important
    )

  "#KappaEditor" -(
    overflowY.auto important
    )

  "#Simulations" -(
    minWidth(600 px) important
    )

}