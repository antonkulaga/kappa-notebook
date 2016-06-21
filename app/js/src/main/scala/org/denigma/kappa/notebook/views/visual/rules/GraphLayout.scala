package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.threejs.PerspectiveCamera

/**
  * Created by antonkulaga on 21/06/16.
  */
trait GraphLayout
{

  /*
  type Node
  type Edge

  def nodes: Vector[Node]
  def edges: Vector[Edge]
  */

  def active: Boolean
  def tick(width: Double, height: Double, camera: PerspectiveCamera): Unit //ticks
  def stop(): Unit
  def pause(): Unit
  def start(width: Double, height: Double, camera: PerspectiveCamera): Unit
}
