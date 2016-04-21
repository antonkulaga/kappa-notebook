package org.denigma.kappa.notebook.views.visual


import org.denigma.binding.extensions.{sq, _}
import org.denigma.binding.views.BindableView
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.Agent
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._


import scala.collection.immutable

class GraphView(val elem: Element,
                val nodes: Rx[Vector[KappaNode]],
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

  protected def subscribeUpdates() = {
    nodeUpdates.onChange{ case (removed, added) =>
      removed.foreach(r => viz.removeSprite(r.view))
      added.foreach(a => viz.addSprite(a.view))
    }
    edgeUpdates.onChange{ case (removed, added)=>
      removed.foreach{ case r=>
        viz.removeObject(r.arrow)
        viz.removeObject(r.arrow.line)
        viz.removeSprite(r.view)
      }
      added.foreach{
        case a=>
          viz.addObject(a.arrow)
          viz.addSprite(a.view)
      }
    }

    nodes.now.foreach(a=>viz.addSprite(a.view))
    edges.now.foreach{
      case a=>
        viz.addObject(a.arrow)
        viz.addSprite(a.view)
    }
    layouts.now.foreach(_.start(viz.width, viz.height,viz.camera))
  }


  override def bindView() = {
    super.bindView()
    subscribeUpdates()
    viz.render()
  }
}
