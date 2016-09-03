package org.denigma.kappa.notebook.graph.layouts

import org.denigma.kappa.notebook.graph.Randomizable
import org.denigma.kappa.notebook.graph.layouts.LayoutMode.LayoutMode
import org.denigma.threejs.{PerspectiveCamera, Vector3}
import org.scalajs.dom
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe

import scala.collection.immutable._

/**
  * Configuration parameters for Force Layout
  */
object ForceLayoutParams {

  lazy val default2D = ForceLayoutParams(0.9, 100, 1, new Vector3(0.0, 0.0, 0.0))

  lazy val default3D = ForceLayoutParams(0.5, 0.5, 1, new Vector3(0.0, 0.0, 0.0), LayoutMode.ThreeD)

}

case class ForceLayoutParams(
                            //attractionMult: Double,
                            springMult: Double,
                            repulsionMult: Double,
                            gravityMult: Double,
                            center: Vector3,
                            mode: LayoutMode = LayoutMode.TwoD
                            )


trait Force[Node, Edge]
{
   protected def max(v1: Double, v2: Double) = {
    if(v1.isNaN || v1.isInfinite) v2 else if(v2.isNaN ||  v2.isInfinite) v1 else Math.max(v1, v2)
  }

  def tick(width: Double, height: Double, camera: PerspectiveCamera, nodes: Vector[Node], edges: Vector[Edge], forceConstant: Double): Unit

}

object LayoutMode extends Enumeration {
  type LayoutMode = Value
  val ThreeD, TwoD = Value
}


trait ForceNode
{

  def layoutInfo: LayoutInfo
  def position: Vector3

}

trait ForceEdge {

  type FromNode <: ForceNode
  type ToNode <: ForceNode

  def from: FromNode
  def to: ToNode

  def update(): Unit
}

trait ForceLayout extends GraphLayout  with Randomizable
{

  type Node <: ForceNode //= AgentNode
  type Edge <: ForceEdge //= KappaEdge

  def mode: LayoutMode
  def forces: Vector[Force[Node, Edge]]

  def nodes: Rx[Vector[Node]]
  def edges: Rx[Vector[Edge]]

  var EPSILON = 0.01

  lazy val maxIterations = 1000
  val layoutIterations: Var[Double] = Var(0)
  val temperature = Rx{
    1 - layoutIterations() / maxIterations
  }

  def info(node: Node): LayoutInfo = node.layoutInfo

  def defRandomDistance = 250

  def randomPos()=  mode match {
    case LayoutMode.TwoD =>
      new Vector3(rand(defRandomDistance), rand(defRandomDistance), 0.0)
      //new Vector3(0, 0, 0.0)

    case LayoutMode.ThreeD => new Vector3(rand(defRandomDistance), rand(defRandomDistance), rand(defRandomDistance))
  }

  private var _active = false
  def active_=(value:Boolean) = if(_active!=value){
    _active = value
  }

  def active = _active

  protected def randomize(nds: Vector[Node]) = {
    for(n <- nds) info(n).init(randomPos())
  }

  def start(width: Double, height: Double, camera: PerspectiveCamera): Unit = {
    randomize(nodes.now)
    layoutIterations() = 0
    active = true
  }

  def tick(width: Double, height: Double, camera: PerspectiveCamera) = {
    if(keepGoing(nodes.now.size))
    {
      var forceConstant: Double = 1.0//Math.sqrt(height * width / nodes.now.size) / 50
      for(force <- forces)
      {
        force.tick(width, height, camera, nodes.now, edges.now, forceConstant)
        this.position(nodes.now)
      }
      layoutIterations() = layoutIterations.now + 1
      this.update(this.edges.now)
    }
  }

  def position(nodes: Vector[Node]) = {
    for {i <- nodes.indices} {
      val node = nodes(i)//.view
      //val view = node.view
      val l = info(node)

      val length = Math.max(EPSILON, l.offset.length())
      //println(s"Math.min(length($length), temperature($temperature))")
      //l.pos.x += (l.offset.x / length) * Math.min(length, temperature)
      //l.pos.y += (l.offset.y / length) * Math.min(length, temperature)
      //l.pos.z += (l.offset.z / length) * Math.min(length, temperature)

      l.pos.x += l.offset.x / 3.0 + l.offset.x * temperature.now / 3.0
      l.pos.y += l.offset.y / 3.0 + l.offset.y * temperature.now / 3.0
      l.pos.z += l.offset.z / 3.0 + l.offset.z * temperature.now  / 3.0

      node.position.x -= (node.position.x - l.pos.x)
      node.position.y -= (node.position.y - l.pos.y)
      node.position.z -= (node.position.z - l.pos.z)

      if(node.position.x.isNaN || node.position.y.isNaN || node.position.z.isNaN) {
        //dom.console.error("NANO DETECTED")
        dom.console.error(s"position(${node.position.x} , ${node.position.y}, ${node.position.z})")
        throw new Exception("invalid argument the position must be:")
      }
      l.setOffsets(0, 0, 0)
    }
  }

  def update(edges: Vector[Edge]) = {

    edges.foreach(e=> e.update())

  }

  def keepGoing(size: Int): Boolean  = size>0 && layoutIterations.now < this.maxIterations && temperature.now > 0.000001

  def pause() = {
    active = false
  }

  /**
   * Stops the calculation by setting the current_iterations to max_iterations.
   */
  def stop() =
  {
    layoutIterations() = this.maxIterations
    active = false
  }

}
