package org.denigma.kappa.notebook.graph.layouts

import org.denigma.kappa.notebook.graph.drawing.Rectangle
import org.denigma.threejs.{PerspectiveCamera, Vector3}
import org.scalajs.dom



class Repulsion[Node <: ForceNode, Edge <: ForceEdge](val repulsionMult: Double, EPSILON: Double = 0.1)
                                                     (compareRepulstion: (Node, Node) => (Double, Double))
  extends Force[Node, Edge] {


  override def tick(width: Double, height: Double, camera: PerspectiveCamera, nodes: Vector[Node], edges: Vector[Edge], forceConstant: Double) = {
    val repulsion = repulsionMult * forceConstant
    for {i <- nodes.indices}
    {
      val no1 = nodes(i)
      val l1 = no1.layoutInfo
      if(i==0) l1.setOffsets(0, 0, 0)

      l1.force = 0
      l1.init(no1.position)

      for {j <- (i + 1) until  nodes.size; if i != j} {
        val no2 = nodes(j)
        val l2 = no2.layoutInfo
        l2.init(no2.position)

        val deltaX = l1.pos.x - l2.pos.x
        val deltaY = l1.pos.y - l2.pos.y
        val deltaZ = l1.pos.z - l2.pos.z
        val (m1, m2) = compareRepulstion(no1, no2)

        val distance = max(EPSILON * 5, l1.pos.distanceTo(l2.pos))
        val distSquared  = Math.pow(distance, 2)
        val force1 =  (repulsion * repulsion) * m2 / distSquared

        //dom.console.log(s"t distance ${distance}: (repulsion(${repulsion}) * repulsion(${repulsion})) * m1(${m1}) / distSquared(${distSquared})) = $force1")

        l1.force += force1
        l1.offset.x = l1.offset.x + (deltaX / distance) * force1
        l1.offset.y = l1.offset.y + (deltaY / distance) * force1
        l1.offset.z = l1.offset.z + (deltaZ / distance) * force1

        if(i==0){
          l2.setOffsets(0,0,0)
        }

        val force2 = (repulsion * repulsion)  * m1 / distSquared

        l2.force += force2
        l2.offset.x = l2.offset.x - (deltaX / distance) * force2
        l2.offset.y = l2.offset.y - (deltaY / distance) * force2
        l2.offset.z = l2.offset.z - (deltaZ / distance) * force2
      }
    }
  }

}