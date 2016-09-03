package org.denigma.kappa.notebook.graph

import org.denigma.kappa.model.Change

import scala.collection.immutable._
import org.denigma.kappa.notebook.graph.layouts.{ForceNode, LayoutInfo}

object OrganizedChangeableNode {
  def emptyChangeMap[T] = Map(Change.Added -> Set.empty[T], Change.Removed -> Set.empty[T], Change.Unchanged -> Set.empty[T], Change.Updated -> Set.empty[T])
}
trait OrganizedChangeableNode extends KappaNode with ChangeableNode {
  type ChildNode <: KappaNode
  type ChildEdge <: KappaEdge

  def children: Map[Change.Change, Set[ChildNode]]

  lazy val childrenList: List[ChildNode] = children.values.flatten.toList

  def childEdges: Map[Change.Change, Set[ChildEdge]]

  lazy val childEdgeList: List[ChildEdge] = childEdges.values.flatten.toList

}

trait ChangeableNode extends KappaNode {
  def status: Change.Change
}

trait KappaNode extends ForceNode {

  val view: KappaView

  def layoutInfo: LayoutInfo

  def position = view.container.position
}
