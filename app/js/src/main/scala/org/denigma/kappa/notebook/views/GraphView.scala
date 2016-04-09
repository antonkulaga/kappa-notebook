package org.denigma.kappa.notebook.views

import org.denigma.binding.views.BindableView
import org.scalajs.dom.raw.Element
import rx._

/**
  * Created by antonkulaga on 3/13/16.
  */
class GraphView(val elem: Element) extends BindableView{

  def defWidth = 600 //NAPILNIK
  def defHeight = 300 //TODO: fix

  val active = Var(false)

//  override lazy val graph:VizGraph = new VizGraph(this.container,defWidth,defHeight)



}
