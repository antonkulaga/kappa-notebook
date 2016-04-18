package org.denigma.kappa.notebook.views.visual

import org.denigma.kappa.model.KappaModel
import org.denigma.threejs.Vector3
import rx._

object ForceLayoutParams {

  lazy val default2D = ForceLayoutParams(10, 1, 0.01, new Vector3(0.0, 0.0, 0.0))

  lazy val default3D = ForceLayoutParams(10, 1, 0.02, new Vector3(0.0, 0.0, 0.0))

}
case class ForceLayoutParams(
                            attractionMult: Double,
                            repulsionMult: Double,
                            gravityMult: Double,
                            center: Vector3
                            )

abstract class Force(layout: GraphLayout) {

  def apply(forceConstant: Double): Unit

}

class ForceLayout(

                   val graphNodes: Var[Vector[KappaNode]],
                   val graphEdges: Var[Vector[KappaEdge]],
                   val width: Double, val height: Double, params: ForceLayoutParams, forces: List[Force] = Nil
                   ) extends GraphLayout
{

  type Node = KappaNode
  type Edge = KappaEdge

  def nodes: Vector[KappaNode] = graphNodes.now
  def edges: Vector[KappaEdge] = graphEdges.now

  def info(node: Node): LayoutInfo = node.layoutInfo

  //var info = Map.empty[Node, LayoutInfo]

  /*
  private var _nodes: Vector[Node] = Vector.empty[Node]
  def nodes_=(value: Vector[Node]) = {
    _nodes = value
    info =  value.map(v=>v->info.getOrElse(v, new LayoutInfo())).toMap
  }

  def nodes: Vector[Node] = _nodes
  var edges: Vector[Edge] = Vector.empty[Edge]
*/


  val mode =  "3d"
  val maxIterations = 200


  var EPSILON = 0.00001
  var layoutIterations = 0
  var temperature = width / 50.0
  

  private var _active = false
  def active_=(value:Boolean) = if(_active!=value){
    _active = value
  }

  def active = _active

  def start(): Unit = {
    layoutIterations = 0
    active = true
  }

  def tick() = if(keepGoing(nodes.size))
  {
    var forceConstant = Math.sqrt(this.height * this.width / nodes.size)
    val repulsion = params.repulsionMult * forceConstant
    this.repulse(nodes, repulsion)
    val attraction = params.attractionMult * forceConstant
    this.attract(this.edges, attraction)
    this.gravity(attraction)
    for(force <- forces) force(forceConstant)

    this.position(nodes)
    this.update(this.edges)

  }
  
  

  def repulse(nodes: Vector[Node], repulsion: Double) = {
    for {i <- nodes.indices}
    {
      val no1: Node = nodes(i)
      val n1 = no1.view
      val l1 = info(no1)
      if(i==0) l1.setOffsets(0, 0, 0)

      l1.force = 0
      l1.init(n1.position)

      for {j <- (i + 1) until  nodes.size; if i != j} {
        val no2 = nodes(j)
        val n2 = no2.view
        val l2 =info(no2)
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

  def attract(edges: Vector[Edge], attraction: Double) =
    for {i <- edges.indices}
    {
      val edge = edges(i)
      //val l1 = edge.from.view.layout
      //val l2 = edge.to.view.layout
      val l1 = info(edge.from)
      val l2 = info(edge.to)

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

  def position(nodes: Vector[Node]) = {

    for {i <- nodes.indices} {
      val node = nodes(i)//.view
      val view = node.view
      val l = info(node)

      val length = Math.max(EPSILON, l.offset.length())

      //val length = Math.max(EPSILON, Math.sqrt(l.offset.x * l.offset.x + l.offset.y * l.offset.y))
      //val length_z = Math.max(EPSILON, Math.sqrt(l.offset.z * l.offset.z + l.offset.y * l.offset.y))
      

      l.pos.x += (l.offset.x / length) * Math.min(length, temperature)
      l.pos.y += (l.offset.y / length) * Math.min(length, temperature)
      l.pos.z += (l.offset.z / length) * Math.min(length, temperature)

      view.position.x -= (view.position.x - l.pos.x) / 10
      view.position.y -= (view.position.y - l.pos.y) / 10
      view.position.z -= (view.position.z - l.pos.z) / 10


    }
    temperature *= (1 - (layoutIterations / this.maxIterations))
    layoutIterations += 1

  }

  def gravity(attraction: Double) = {
    for {i <- nodes.indices}
    {
      val no1: Node = nodes(i)
      val n1 = no1.view
      val l1 = info(no1)
      if(i==0) l1.setOffsets(0, 0, 0)

      l1.force = 0
      l1.init(n1.position)

      val deltaX = l1.pos.x - params.center.x
      val deltaY = l1.pos.y - params.center.y
      val deltaZ = l1.pos.z - params.center.z

      val distance = Math.max(EPSILON, l1.pos.distanceTo(params.center))

      val force =  attraction * params.gravityMult / Math.sqrt(distance)

      l1.force += force
      l1.offset.x = l1.offset.x - (deltaX / distance) * force
      l1.offset.y = l1.offset.y - (deltaY / distance) * force
      l1.offset.z = l1.offset.z - (deltaZ / distance) * force
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
