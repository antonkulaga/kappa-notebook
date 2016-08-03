package org.denigma.kappa.notebook.graph

import org.denigma.kappa.notebook.graph.layouts.ForceEdge
import org.denigma.threejs._
import org.denigma.binding.extensions._

trait KappaEdge extends ForceEdge {

  override type FromNode <: KappaNode
  override type ToNode <: KappaNode

  def sourcePos: Vector3 = from.view.container.position
  def targetPos: Vector3 = to.view.container.position

}

trait ArrowEdge extends KappaEdge {


  def lineParams: LineParams

  def direction: Vector3 = new Vector3().subVectors(targetPos, sourcePos)

  def middle: Vector3 = new Vector3((sourcePos.x + targetPos.x) / 2,(sourcePos.y + targetPos.y) / 2, (sourcePos.z + targetPos.z) / 2)

  val arrow = new ArrowHelper(direction.normalize(), sourcePos, direction.length(), lineParams.lineColor, lineParams.headLength, lineParams.headWidth)
  arrow.line.material.dyn.linewidth = lineParams.thickness
  arrow.frustumCulled = false
  arrow.line.frustumCulled = false
  arrow.cone.frustumCulled = false

  protected def posArrow() = {
    arrow.position.set(sourcePos.x, sourcePos.y, sourcePos.z) // = sourcePos
    arrow.setDirection(direction.normalize())
    arrow.setLength(direction.length()-3, lineParams.headLength, lineParams.headWidth)
  }


  def update(): Unit = {
    posArrow()
  }


}
