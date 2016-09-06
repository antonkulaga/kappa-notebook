package org.denigma.kappa.notebook.graph.layouts

import org.denigma.threejs.{PerspectiveCamera, Vector3}


class Attraction[Node <: ForceNode, Edge <: ForceEdge](val attractionMult: Double, EPSILON: Double = 0.001)
                                                      (val compare: (Edge#FromNode, Edge#ToNode) => (Double, Double) )
  extends Force[Node, Edge] {


  override def tick(width: Double, height: Double, camera: PerspectiveCamera, nodes: Vector[Node], edges: Vector[Edge], forceConstant: Double) = {
    val attractionGlobal = attractionMult * forceConstant
    for {i <- edges.indices} {
      val edge = edges(i)
      //val l1 = edge.from.view.layout
      //val l2 = edge.to.view.layout
      val l1 = edge.from.layoutInfo
      val l2 = edge.to.layoutInfo

      val deltaX = l1.pos.x - l2.pos.x
      val deltaY = l1.pos.y - l2.pos.y
      val deltaZ = l1.pos.z - l2.pos.z

      val distance = max(EPSILON, l1.pos.distanceTo(l2.pos))
      val (m1, m2) = compare(edge.from, edge.to)

      val force1= distance  / (attractionGlobal * m2)
      val force2 = distance  / (attractionGlobal * m1)

      //l1.force -= force1
      //l2.force += force2

      l1.offset.x -= deltaX  * force1
      l1.offset.y -= deltaY  * force1
      l1.offset.z -= deltaZ  * force1


      l2.offset.x += deltaX  * force2
      l2.offset.y += deltaY  * force2
      l2.offset.z += deltaZ  * force2
    }
  }

}

class Gravity[Node <: ForceNode, Edge <: ForceEdge](val gravityMult: Double, center: Vector3,  EPSILON: Double = 0.01) extends Force[Node, Edge] {

  override def tick(width: Double, height: Double, camera: PerspectiveCamera, nodes: Vector[Node], edges: Vector[Edge], forceConstant: Double) = {
    val attraction = gravityMult * forceConstant
    for {i <- nodes.indices}
    {
      val no1 = nodes(i)
      val l1 = no1.layoutInfo
      //if(i==0) l1.setOffsets(0, 0, 0)

      //l1.force = 0
      l1.fillIfEmpty(no1.position)

      val deltaX = l1.pos.x - center.x
      val deltaY = l1.pos.y - center.y
      val deltaZ = l1.pos.z - center.z

      val distance = Math.max(1, l1.pos.distanceTo(center))//Math.max(EPSILON, l1.pos.distanceTo(center))

      val force =  attraction * gravityMult / Math.sqrt(distance)

      //l1.force += force
      l1.offset.x = l1.offset.x - (deltaX / distance) * force
      l1.offset.y = l1.offset.y - (deltaY / distance) * force
      l1.offset.z = l1.offset.z - (deltaZ / distance) * force
    }
  }

}
