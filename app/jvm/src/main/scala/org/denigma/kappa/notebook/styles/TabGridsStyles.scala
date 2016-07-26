package org.denigma.kappa.notebook.styles
import scalacss.Defaults._
import scalacss.Defaults._
trait TabGridsStyles extends MainStyles {
  import dsl._

  ".ui.column" -(
    overflowY.auto,
    padding(0 px)  important
    )

  ".ui.number.input" -(
    maxWidth(40 px)
    )


  ".page.tab" - (
    overflowY.hidden important,
    overflowX.hidden important
    )

  "wide tab segment" - (
    width(100 %%)
    )

  ".tab.page" -(
    overflowY.hidden important,
    overflowX.hidden important,
    //overflowX.scroll,
    minHeight(80.0 vh) important
    //height(100.0 %%)
    )

  ".tab.subpage" -(
    /*
    onTiny -(height(65 vh) important),
    onLittle -(height(66 vh) important),
    onSmall -(height(68 vh) important),
    onMedium -(height(70 vh) important),
    onLarge -(height(75 vh) important)
    */
    height(68 vh) important,
    overflow.visible important
    //overflowY.scroll
    )

  ".tab.smallpage" -(
    overflowY.auto,
    onTiny -(maxHeight(50 vh) important),
    onLittle -(maxHeight(53 vh) important),
    onSmall -(maxHeight(55 vh) important),
    onMedium -(maxHeight(58 vh) important),
    onLarge -(maxHeight(60 vh) important)
    )

  ".tab.flexible.page" -(
    overflowY.auto,
    overflowX.auto,
    height(100.0 %%)
    )

  ".simulation.tab" -(
    overflowX.visible,
    overflowY.visible
    )

  "#ProjectsPanel" -(
    minWidth(345 px) important
    )

}