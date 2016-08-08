package org.denigma.kappa.notebook.graph

import org.denigma.kappa.notebook.graph.layouts.ForceEdge
import org.denigma.threejs._
import org.denigma.binding.extensions._
import org.denigma.kappa.model.Change

trait KappaEdge extends ForceEdge {

  override type FromNode <: KappaNode
  override type ToNode <: KappaNode

  def sourcePos: Vector3 = from.view.container.position
  def targetPos: Vector3 = to.view.container.position

}

trait ChangeableEdge extends ArrowEdge {

  def status: Change.Value

  def opacity: Double = arrow.line.material.opacity
  def opacity_=(value: Double) = {
    arrow.cone.material.opacity = value
    arrow.line.material.opacity = value
  }
}

trait ArrowEdge extends KappaEdge {

  def lineParams: LineParams

  def direction: Vector3 = new Vector3().subVectors(targetPos, sourcePos)

  def middleDivider: Double = 2

  def middle: Vector3 = new Vector3((sourcePos.x + targetPos.x) / middleDivider, (sourcePos.y + targetPos.y) / middleDivider, (sourcePos.z + targetPos.z) / middleDivider)

  val arrow = new ArrowHelper(direction.normalize(), sourcePos, direction.length(), lineParams.lineColor, lineParams.headLength, lineParams.headWidth)
  arrow.line.material.dyn.linewidth = lineParams.thickness

  protected def posArrow() = {
    arrow.position.set(sourcePos.x, sourcePos.y, sourcePos.z) // = sourcePos
    arrow.setDirection(direction.normalize())
    arrow.setLength(direction.length(), lineParams.headLength, lineParams.headWidth)
  }


  def update(): Unit = {
    posArrow()
  }


}
