package org.denigma.kappa.notebook.views.visual.utils

import org.denigma.threejs.{Vector2, Vector3}

/**
 * Trait that can generate random values
 */

trait Randomizable
{

  //def defRandomDistance: Double = 1000

  def rand(randomDistance: Double) = (0.5-Math.random()) * randomDistance
  def rand2(randomDistance: Double) = new Vector2(rand(randomDistance), rand(randomDistance))
  def rand3(randomDistance: Double) = new Vector3(rand(randomDistance), rand(randomDistance), rand(randomDistance))
  def randColor() = Math.random() * 0x1000000

}
