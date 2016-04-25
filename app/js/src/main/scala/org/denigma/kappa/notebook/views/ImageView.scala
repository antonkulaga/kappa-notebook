package org.denigma.kappa.notebook.views

import org.denigma.binding.views.BindableView
import org.denigma.kappa.notebook.KappaHub
//import org.denigma.controls.tabs.TabItem
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.kappa.notebook.views.common.TabItem
import org.scalajs.dom.raw.Element

class ImageView(val elem: Element, val selected: Var[String], hub: KappaHub) extends BindableView with TabItem
{

  val imageName: Rx[String] = hub.selectedImage

  val src = imageName.map(i => "files/"+imageName)

}
