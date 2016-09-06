package org.denigma.kappa.notebook.graph.layouts

import org.denigma.threejs.Vector3

//TODO: fix this terrible imperative code in the future
class LayoutInfo(val mass: Double = 1.0, protected val _pos: Vector3 = new Vector3(0, 0, 0), protected val _offset: Vector3 = new Vector3(0, 0, 0))
{
  //var force: Double = 0

  def fillIfEmpty(v: Vector3) =  {
    if(pos.x ==0 && pos.y==0.0 && (_pos.z==0 || _pos.z.isNaN || _pos.z.isInfinity)){ pos = v}
    pos
  }

  def pos = _pos
  def pos_=(v: Vector3): Unit = {
    _pos.x = v.x
    _pos.y = v.y
    _pos.z = v.z
  }

  def pos_=(xyz: (Double, Double, Double)): Unit = xyz match { case (x, y, z) =>
    pos = new Vector3(x, y, z)
  }

  def offset = _offset

  def offset_=(v: Vector3): Unit = {
    _offset.x = v.x
    _offset.y = v.y
    _offset.z = v.z
  }

  def offset_=(xyz: (Double, Double, Double)): Unit = xyz match { case (x, y, z) =>
    offset = new Vector3(x, y, z)
  }
def clean() = {
    pos = (0.0, 0.0, 0.0)
    offset = (0.0, 0.0, 0.0)
  //force = 0
  }
}
