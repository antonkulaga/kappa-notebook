package org.denigma.kappa.notebook.graph.layouts

import org.denigma.kappa.notebook.graph.Randomizable
import org.denigma.kappa.notebook.graph.layouts.LayoutMode.LayoutMode
import org.denigma.threejs.{PerspectiveCamera, Vector3}
import rx._

import scala.collection.immutable._

object ForceLayoutParams {

  lazy val default2D = ForceLayoutParams(50, 0.8, 0.01, new Vector3(0.0, 0.0, 0.0))

  lazy val default3D = ForceLayoutParams(50, 0.8, 0.01, new Vector3(0.0, 0.0, 0.0))

}

case class ForceLayoutParams(
                            attractionMult: Double,
                            repulsionMult: Double,
                            gravityMult: Double,
                            center: Vector3,
                            mode: LayoutMode = LayoutMode.TwoD
                            )


trait Force[Node, Edge]
{
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

  def info(node: Node): LayoutInfo = node.layoutInfo

  def defRandomDistance = 50

  def randomPos()=  mode match {
    case LayoutMode.TwoD =>
      new Vector3(rand(defRandomDistance), rand(defRandomDistance), 0.0)
      //new Vector3(0, 0, 0.0)

    case LayoutMode.ThreeD => new Vector3(rand(defRandomDistance), rand(defRandomDistance), rand(defRandomDistance))
  }

  val maxIterations = 400

  var EPSILON = 0.00001

  var layoutIterations = 0

  var temperature = 1000 / 50.0
  

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
    var temperature = width / 50.0
    layoutIterations = 0
    active = true
  }

  def tick(width: Double, height: Double, camera: PerspectiveCamera) = {
    if(keepGoing(nodes.now.size))
    {
      var forceConstant: Double = Math.sqrt(height * width / nodes.now.size)

      for(force <- forces)
      {
        force.tick(width, height, camera, nodes.now, edges.now, forceConstant)
        this.position(nodes.now)
      }
      temperature *= (1 - (layoutIterations / this.maxIterations))
      layoutIterations += 1
      this.update(this.edges.now)
    }
  }

  def position(nodes: Vector[Node]) = {
    for {i <- nodes.indices} {
      val node = nodes(i)//.view
      //val view = node.view
      val l = info(node)

      val length = Math.max(EPSILON, l.offset.length())
      l.pos.x += (l.offset.x / length) * Math.min(length, temperature)
      l.pos.y += (l.offset.y / length) * Math.min(length, temperature)
      l.pos.z += (l.offset.z / length) * Math.min(length, temperature)

      node.position.x -= (node.position.x - l.pos.x) / 10
      node.position.y -= (node.position.y - l.pos.y) / 10
      node.position.z -= (node.position.z - l.pos.z) / 10
    }
  }

  def update(edges: Vector[Edge]) = {

    edges.foreach(e=> e.update())

  }

  def keepGoing(size: Int): Boolean  = size>0 && layoutIterations < this.maxIterations && temperature > 0.000001

  def pause() = {
    active = false
  }

  /**
   * Stops the calculation by setting the current_iterations to max_iterations.
   */
  def stop() =
  {
    layoutIterations = this.maxIterations
    active = false
  }

}
