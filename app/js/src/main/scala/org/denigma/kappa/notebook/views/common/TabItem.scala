package org.denigma.kappa.notebook.views.common

import org.denigma.binding.views.BindableView
import rx.Ctx.Owner.Unsafe.Unsafe
import rx.Rx

/**
  * Created by antonkulaga on 1/2/16.
  */
trait TabItem {
  self: BindableView =>

  def selected: Rx[String]

  lazy val active: rx.Rx[Boolean] = selected.map(value => value == self.id)
}
