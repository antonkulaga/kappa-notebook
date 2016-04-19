package org.denigma.kappa.notebook.views.visual

import org.denigma.threejs.PerspectiveCamera

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