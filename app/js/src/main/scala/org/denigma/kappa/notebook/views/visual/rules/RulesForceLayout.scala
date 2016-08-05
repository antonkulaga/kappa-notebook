package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.kappa.notebook.graph.layouts.{Force, ForceLayout}
import org.denigma.kappa.notebook.graph.layouts.LayoutMode.LayoutMode
import rx._
import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.graph.{ChangeableEdge, KappaEdge, KappaNode}

class RulesForceLayout(
                   val nodes: Rx[Vector[KappaNode]],
                   val edges: Rx[Vector[ChangeableEdge]],
                   val mode: LayoutMode,
                   val forces: Vector[Force[KappaNode, ChangeableEdge]]
                 ) extends ForceLayout
{
  override type Node = KappaNode

  override type Edge = ChangeableEdge

  nodes.removedInserted.onChange{
    case (rs, is)=>
      randomize(is.toVector)
      layoutIterations() = 0
      active = true
  }
}