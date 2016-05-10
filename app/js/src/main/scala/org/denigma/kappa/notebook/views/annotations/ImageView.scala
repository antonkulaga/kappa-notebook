package org.denigma.kappa.notebook.views.annotations

import org.denigma.binding.views.BindableView
import org.denigma.kappa.notebook.views.common.TabItem
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

class ImageView(val elem: Element, val selected: Var[String], val imageName: Rx[String]) extends BindableView with TabItem
{

  val src = imageName.map(i => "files/"+imageName)

}
