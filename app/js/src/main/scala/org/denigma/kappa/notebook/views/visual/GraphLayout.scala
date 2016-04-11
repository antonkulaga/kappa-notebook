package org.denigma.kappa.notebook.views.visual

trait GraphLayout
{

  type Node
  type Edge

  var nodes: Seq[Node]
  var edges: Seq[Edge]

  def width: Double
  def height: Double
  def active: Boolean
  def start(nodes: Seq[Node], edges: Seq[Edge]):Unit
  def tick(): Unit
  def stop(): Unit
  def pause(): Unit

}