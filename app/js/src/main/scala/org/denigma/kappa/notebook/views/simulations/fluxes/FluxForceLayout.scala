package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.kappa.notebook.graph.layouts.ForceLayout


import org.denigma.kappa.notebook.graph.layouts.{Force, ForceLayout}
import org.denigma.kappa.notebook.graph.layouts.LayoutMode.LayoutMode
import rx._
import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.graph.{KappaEdge, KappaNode}

class FluxForceLayout(

                        val nodes: Rx[Vector[FluxNode]],
                        val edges: Rx[Vector[FluxEdge]],
                        val mode: LayoutMode,
                        val forces: Vector[Force[FluxNode, FluxEdge]]
                      ) extends ForceLayout
{
  override type Node = FluxNode

  override type Edge = FluxEdge

  nodes.removedInserted.onChange{
    case (rs, is)=>
      randomize(is.toVector)
      var temperature = 500 / 50.0
      layoutIterations = 0
      active = true
  }
}