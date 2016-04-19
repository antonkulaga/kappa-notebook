package org.denigma.kappa.notebook.views.visual

import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.notebook.views.visual.LayoutMode.LayoutMode
import org.denigma.threejs.{PerspectiveCamera, Vector3}
import rx._

object ForceLayoutParams {

  lazy val default2D = ForceLayoutParams(10, 0.2, 0.01, new Vector3(0.0, 0.0, 0.0))

  lazy val default3D = ForceLayoutParams(10, 0.2, 0.02, new Vector3(0.0, 0.0, 0.0))

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

class Repulsion(val repulsionMult: Double, EPSILON: Double = 0.00001) extends Force[KappaNode, KappaEdge] {

  override def tick(width: Double, height: Double, camera: PerspectiveCamera, nodes: Vector[KappaNode], edges: Vector[KappaEdge], forceConstant: Double) = {
    val repulsion = repulsionMult * forceConstant
    for {i <- nodes.indices}
    {
      val no1 = nodes(i)
      val n1 = no1.view
      val l1 = no1.layoutInfo
      if(i==0) l1.setOffsets(0, 0, 0)

      l1.force = 0
      l1.init(n1.position)

      for {j <- (i + 1) until  nodes.size; if i != j} {
        val no2 = nodes(j)
        val n2 = no2.view
        val l2 = no2.layoutInfo
        l2.init(n2.position)

        val deltaX = l1.pos.x - l2.pos.x
        val deltaY = l1.pos.y - l2.pos.y
        val deltaZ = l1.pos.z - l2.pos.z

        val distance = Math.max(EPSILON, l1.pos.distanceTo(l2.pos))


        val force =  (repulsion * repulsion) / Math.pow(distance, 2)
        l1.force += force
        l1.offset.x = l1.offset.x + (deltaX / distance) * force
        l1.offset.y = l1.offset.y + (deltaY / distance) * force
        l1.offset.z = l1.offset.z + (deltaZ / distance) * force

        if(i==0){
          l2.setOffsets(0,0,0)
        }

        l2.force += force
        l2.offset.x = l2.offset.x - (deltaX / distance) * force
        l2.offset.y = l2.offset.y - (deltaY / distance) * force
        l2.offset.z = l2.offset.z - (deltaZ / distance) * force
      }
    }
  }

}

class Attraction(val attractionMult: Double, EPSILON: Double = 0.00001) extends Force[KappaNode, KappaEdge] {


  override def tick(width: Double, height: Double, camera: PerspectiveCamera, nodes: Vector[KappaNode], edges: Vector[KappaEdge], forceConstant: Double) = {
    val attraction = attractionMult * forceConstant
    for {i <- edges.indices} {
      val edge = edges(i)
      //val l1 = edge.from.view.layout
      //val l2 = edge.to.view.layout
      val l1 = edge.from.layoutInfo
      val l2 = edge.to.layoutInfo

      val deltaX = l1.pos.x - l2.pos.x
      val deltaY = l1.pos.y - l2.pos.y
      val deltaZ = l1.pos.z - l2.pos.z

      val distance = Math.max(EPSILON, l1.pos.distanceTo(l2.pos))

      val force = (distance * distance) / attraction

      l1.force -= force
      l2.force += force

      l1.offset.x -= (deltaX / distance) * force
      l1.offset.y -= (deltaY / distance) * force
      l1.offset.z -= (deltaZ / distance) * force


      l2.offset.x += (deltaX / distance) * force
      l2.offset.y += (deltaY / distance) * force
      l2.offset.z += (deltaZ / distance) * force
    }
  }

}

class BorderForce(val repulsionMult: Double, val threshold: Double, mult: Double, center: Vector3) extends Force[KappaNode, KappaEdge] {

  def border(width: Double, height: Double) = Rectangle.fromCorners(center.x - width / 2, center.y - height / 2, center.x + width /2, center.y + height / 2)

  def toHorBorders(rect: Rectangle, x: Double) = (x - rect.left, rect.right - x)

  def toVerBorders(rect: Rectangle, y: Double) = (y - rect.top,  rect.bottom - y)

  def toBorder: PartialFunction[(Double, Double), Double] = {
    case (ld, rd) if (ld - threshold) < 0.0  =>
      ld - threshold

    case (ld, rd) if (rd - threshold) < 0.0  =>
      Math.abs(rd - threshold)

    case _ => 0
  }


  override def tick(width: Double, height: Double, camera: PerspectiveCamera, nodes: Vector[KappaNode], edges: Vector[KappaEdge], forceConstant: Double) = {
    println(width, height)
    val repulsion = repulsionMult * forceConstant
    val rect = border(width *  mult, height * mult)
    for {
      i <- nodes.indices
    }
    {
      val no1 = nodes(i)
      val n1 = no1.view
      val l1 = no1.layoutInfo
      if(i==0) l1.setOffsets(0, 0, 0)

      val leftD = rect.x - threshold - l1.pos.y

      val deltaX = toBorder(toHorBorders(rect, l1.pos.x))
      val deltaY = toBorder(toVerBorders(rect, l1.pos.y))

      val deltaZ = 0

      val distance = Math.max(Math.abs(deltaX), Math.abs(deltaY))
      if(distance > 0) {
        val force =  (repulsion * repulsion) / Math.pow(distance, 2)
        l1.force += force
        l1.offset.x = l1.offset.x - (deltaX / distance) * force
        l1.offset.y = l1.offset.y - (deltaY / distance) * force
        l1.offset.z = 0
      }
    }
  }

}


class Gravity(val attractionMult: Double, val gravityMult: Double, center: Vector3,  EPSILON: Double = 0.00001) extends Force[KappaNode, KappaEdge] {

  override def tick(width: Double, height: Double, camera: PerspectiveCamera, nodes: Vector[KappaNode], edges: Vector[KappaEdge], forceConstant: Double) = {
      val attraction = attractionMult * forceConstant
      for {i <- nodes.indices}
      {
        val no1 = nodes(i)
        val n1 = no1.view
        val l1 = no1.layoutInfo
        if(i==0) l1.setOffsets(0, 0, 0)

        l1.force = 0
        l1.init(n1.position)

        val deltaX = l1.pos.x - center.x
        val deltaY = l1.pos.y - center.y
        val deltaZ = l1.pos.z - center.z

        val distance = Math.max(EPSILON, l1.pos.distanceTo(center))

        val force =  attraction * gravityMult / Math.sqrt(distance)

        l1.force += force
        l1.offset.x = l1.offset.x - (deltaX / distance) * force
        l1.offset.y = l1.offset.y - (deltaY / distance) * force
        l1.offset.z = l1.offset.z - (deltaZ / distance) * force
      }
    }

}


class ForceLayout(

                   val graphNodes: Var[Vector[KappaNode]],
                   val graphEdges: Var[Vector[KappaEdge]],
                   val mode: LayoutMode,
                   val forces: Vector[Force[KappaNode, KappaEdge]]
                 ) extends GraphLayout  with Randomizable
{

  type Node = KappaNode
  type Edge = KappaEdge

  def nodes: Vector[KappaNode] = graphNodes.now
  def edges: Vector[KappaEdge] = graphEdges.now

  def info(node: Node): LayoutInfo = node.layoutInfo

  override def defRandomDistance = 400

  def randomPos()=  mode match {
    case LayoutMode.TwoD =>new Vector3(rand(), rand(), 0.0)
    case LayoutMode.ThreeD => new Vector3(rand(), rand(), rand())
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

  def start(width: Double, height: Double, camera: PerspectiveCamera): Unit = {

    for(n <- nodes) info(n).init(randomPos())
    var temperature = width / 50.0
    layoutIterations = 0
    active = true
  }

  def tick(width: Double, height: Double, camera: PerspectiveCamera) = if(keepGoing(nodes.size))
  {
    var forceConstant: Double = Math.sqrt(height * width / nodes.size)

    for(force <- forces)
    {
      force.tick(width, height, camera, nodes, edges, forceConstant)
      this.position(nodes)
    }
    temperature *= (1 - (layoutIterations / this.maxIterations))
    layoutIterations += 1
    this.update(this.edges)
  }

  def position(nodes: Vector[Node]) = {
    for {i <- nodes.indices} {
      val node = nodes(i)//.view
      val view = node.view
      val l = info(node)

      val length = Math.max(EPSILON, l.offset.length())
      l.pos.x += (l.offset.x / length) * Math.min(length, temperature)
      l.pos.y += (l.offset.y / length) * Math.min(length, temperature)
      l.pos.z += (l.offset.z / length) * Math.min(length, temperature)

      view.position.x -= (view.position.x - l.pos.x) / 10
      view.position.y -= (view.position.y - l.pos.y) / 10
      view.position.z -= (view.position.z - l.pos.z) / 10
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
