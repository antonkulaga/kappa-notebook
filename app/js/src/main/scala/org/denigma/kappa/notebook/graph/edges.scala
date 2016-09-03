package org.denigma.kappa.notebook.graph

import org.denigma.kappa.notebook.graph.layouts.{ForceEdge, ForceNode}
import org.denigma.threejs._
import org.denigma.binding.extensions._
import org.denigma.kappa.model.Change

import scala.scalajs.js

trait KappaEdge extends ForceEdge {


  override type FromNode <: ForceNode//KappaNode
  override type ToNode <: ForceNode//KappaNode

  def sourcePos: Vector3 = from.position
  def targetPos: Vector3 = to.position

}

trait ChangeableEdge extends LineEdge {

  def status: Change.Value

}

trait LineEdge extends KappaEdge {

  self =>

  def opacity: Double = line.material.opacity
  def opacity_=(value: Double) = if(line.material.opacity!= value) {
    line.material.opacity = value
  }

  def lineParams: LineParams

  def direction: Vector3 = new Vector3().subVectors(targetPos, sourcePos)

  def middleDivider: Double = 2

  def middle: Vector3 = new Vector3((sourcePos.x + targetPos.x) / middleDivider, (sourcePos.y + targetPos.y) / middleDivider, (sourcePos.z + targetPos.z) / middleDivider)

  lazy protected val parameters  = js.Dynamic.literal(color = lineParams.lineColor, linewidth = lineParams.thickness).asInstanceOf[LineBasicMaterialParameters]
  lazy val material = new LineBasicMaterial( parameters )
  lazy val line = new Line(makeGeometry(), material)

  protected def makeGeometry() = {
    val geometry = new Geometry()
    geometry.vertices.push(from.position)
    geometry.vertices.push(middle)
    geometry.vertices.push(to.position)
    geometry
  }

  protected def posLine() = {
    line.geometry = makeGeometry()
    line.geometry.dynamic = true
    line.geometry.verticesNeedUpdate = true
  }


  def update(): Unit = {
    posLine()
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
    val dir = direction
    arrow.position.set(sourcePos.x, sourcePos.y, sourcePos.z) // = sourcePos
    arrow.setDirection(dir.normalize())
    val dist = targetPos.distanceTo(sourcePos)
    arrow.setLength(dist, lineParams.headLength, lineParams.headWidth)
  }


  def update(): Unit = {
    posArrow()
  }


}
