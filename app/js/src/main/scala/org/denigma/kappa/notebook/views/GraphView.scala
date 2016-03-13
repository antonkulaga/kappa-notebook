package org.denigma.kappa.notebook.views

import org.denigma.binding.views.BindableView
import org.w3c.dom.html.HTMLElement

/**
  * Created by antonkulaga on 3/13/16.
  */
class GraphView(val elem: HTMLElement) extends BindableView{


  def defWidth = 600 //NAPILNIK
  def defHeight = 300 //TODO: fix

  override lazy val graph:VizGraph = new VizGraph(this.container,defWidth,defHeight)



}
