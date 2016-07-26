package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.kappa.notebook.graph.layouts.{Force, ForceLayout}
import org.denigma.kappa.notebook.graph.layouts.LayoutMode.LayoutMode
import rx._
import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.graph.{KappaEdge, KappaNode}

class RulesForceLayout(
                   val nodes: Rx[Vector[KappaNode]],
                   val edges: Rx[Vector[KappaEdge]],
                   val mode: LayoutMode,
                   val forces: Vector[Force[KappaNode, KappaEdge]]
                 ) extends ForceLayout
{
  override type Node = KappaNode

  override type Edge = KappaEdge

  nodes.removedInserted.onChange{
    case (rs, is)=>
      randomize(is.toVector)
      var temperature = 500 / 50.0
      layoutIterations = 0
      active = true
  }
}