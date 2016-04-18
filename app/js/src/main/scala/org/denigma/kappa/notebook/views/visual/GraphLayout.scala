package org.denigma.kappa.notebook.views.visual

trait GraphLayout
{

  /*
  type Node
  type Edge

  def nodes: Vector[Node]
  def edges: Vector[Edge]
  */

  def width: Double
  def height: Double
  def active: Boolean
  def tick(): Unit
  def stop(): Unit
  def pause(): Unit
  def start(): Unit
}