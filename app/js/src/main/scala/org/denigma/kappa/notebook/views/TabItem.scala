package org.denigma.kappa.notebook.views

import org.denigma.binding.views.BindableView
import rx.Rx
import rx.ops._
/**
  * Created by antonkulaga on 1/2/16.
  */
trait TabItem {
  self: BindableView =>
  def selected: Rx[String]
  val active: rx.Rx[Boolean] = selected.map(value => value == self.id)
}
