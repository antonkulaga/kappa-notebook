package org.denigma.kappa.notebook.graph


import org.denigma.kappa.notebook.graph.layouts.{ForceNode, LayoutInfo}


trait KappaOrganizedNode extends KappaNode {
  type ChildNode <: KappaNode

  def children: List[ChildNode]

}

trait KappaNode extends ForceNode {

  val view: KappaView

  def layoutInfo: LayoutInfo

  def position = view.container.position
}
