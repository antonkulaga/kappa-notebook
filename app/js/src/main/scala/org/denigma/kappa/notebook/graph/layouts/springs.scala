package org.denigma.kappa.notebook.graph.layouts
import org.denigma.threejs.PerspectiveCamera


case class SpringParams(length: Double, strength: Double = 1, mass1: Double = 1, mass2: Double = 1)

class SpringForce[Node <: ForceNode, Edge <: ForceEdge](val springMult: Double, EPSILON: Double = 0.01)
                                                       (val compute: (Edge) => SpringParams )
  extends Force[Node, Edge] {

  override def tick(width: Double, height: Double, camera: PerspectiveCamera, nodes: Vector[Node], edges: Vector[Edge], forceConstant: Double) = {
    val attractionGlobal = springMult * forceConstant

    for {i <- edges.indices} {
      val edge = edges(i)
      val l1 = edge.from.layoutInfo
      val l2 = edge.to.layoutInfo

      val deltaX = l1.pos.x - l2.pos.x
      val deltaY = l1.pos.y - l2.pos.y
      val deltaZ = l1.pos.z - l2.pos.z

      val distance = max(1, l1.pos.distanceTo(l2.pos))//max(EPSILON, l1.pos.distanceTo(l2.pos))
      val SpringParams(length, strength, mass1, mass2) = compute(edge)

      val force1= (distance - length)  * strength * mass2 * attractionGlobal / 2
      val force2 = (distance - length) * strength * mass1 *  attractionGlobal / 2
      println(s"SPRING FORCES ${force1} ${force2}")

      //l1.force -= force1
      //l2.force += force2

      l1.offset.x -= (deltaX / distance)  * force1
      l1.offset.y -= (deltaY / distance)  * force1
      l1.offset.z -= (deltaZ  / distance) * force1


      l2.offset.x += (deltaX / distance)  * force2
      l2.offset.y += (deltaY / distance) * force2
      l2.offset.z += (deltaZ /distance) * force2
      //dom.console.log(s"SPRING = $force1(${force1}) force2(${force2}) offsets(${l1.offset.x} , ${l1.offset.y}, ${l1.offset.z})")
    }
  }
}
