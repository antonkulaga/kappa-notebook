package org.denigma.kappa.notebook.views

import org.denigma.binding.views.BindableView
import org.scalajs.dom.raw.Element
import rx._
import org.querki.jquery._
import org.denigma.binding.extensions._
import rx.Ctx.Owner.Unsafe.Unsafe
/**
 * View for the sitebar
 */
class SidebarView (val elem:Element) extends BindableView {

  val logo = Var("/resources/logo.jpg")

  override def bindElement(el:Element) = {
    super.bindElement(el)
    $(".ui.accordion").dyn.accordion()
  }

}
