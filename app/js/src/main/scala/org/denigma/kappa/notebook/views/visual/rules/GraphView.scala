package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.binding.extensions.{sq, _}
import org.denigma.binding.views.BindableView
import org.denigma.kappa.notebook.views.visual.rules.layouts.GraphLayout
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

class GraphView(val elem: Element,
                val nodes: Rx[Vector[AgentNode]],
                val edges: Rx[Vector[KappaEdge]],
                val layouts: Rx[Vector[GraphLayout]],
                val containerName: String
               ) extends BindableView
{

  val active: Rx[Boolean] = Rx{
    nodes().nonEmpty
  }// Var(false)

  protected def defaultWidth: Double = elem.getBoundingClientRect().width

  protected def defaultHeight: Double = Math.max(150.0, dom.window.innerHeight / 4)

  val container = sq.byId(containerName).get

  val nodeUpdates = nodes.removedInserted

  val edgeUpdates = edges.removedInserted

  val viz = new Visualizer(container,
    defaultWidth,
    defaultHeight,
    layouts,
    500.0
  )

  protected def onAddNode(node: AgentNode) = {
    viz.addSprite(node.view)
  }

  protected def onRemoveNode(node: AgentNode) = {
    node.clearChildren()
    viz.removeSprite(node.view)
  }

  protected def onAddEdge(edge: KappaEdge) = {
    viz.addSprite(edge.view)
    viz.addObject(edge.arrow)
  }

  protected def onRemoveEdge(edge: KappaEdge) = {
    edge.clearChildren()
    viz.removeObject(edge.arrow)
    viz.removeSprite(edge.view)
    //println("removed edge with :" + viz.cssScene.children.contains(edge.view))
  }

  protected def subscribeUpdates() = {
    nodeUpdates.onChange{ case (removed, added) =>
      removed.foreach(onRemoveNode)
      added.foreach(onAddNode)
    }
    edgeUpdates.onChange{ case (removed, added)=>
      removed.foreach(onRemoveEdge)
      added.foreach(onAddEdge)
    }
    nodes.now.foreach(onAddNode)
    edges.now.foreach(onAddEdge)
    layouts.now.foreach(_.start(viz.width, viz.height, viz.camera))
  }


  override def bindView() = {
    super.bindView()
    subscribeUpdates()
    viz.render()
  }
}
