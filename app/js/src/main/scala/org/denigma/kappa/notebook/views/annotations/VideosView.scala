package org.denigma.kappa.notebook.views.annotations

import org.denigma.binding.views.BindableView
import org.scalajs.dom.raw.Element
import rx._
import org.denigma.kappa.notebook.views.common.TabItem


/**
  * Created by antonkulaga on 5/10/16.
  */
class VideosView(val elem: Element, val selected: Var[String], val src: Rx[String]) extends BindableView{

}

class VidView(val elem: Element, val selected: Var[String], val src: Rx[String]) extends BindableView with TabItem
{
}