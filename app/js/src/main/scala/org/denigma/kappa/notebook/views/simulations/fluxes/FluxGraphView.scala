package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts._
import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, Element, HTMLElement}
import org.scalajs.dom.svg.SVG
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

import scala.Vector
import scala.collection.immutable._


class FluxGraphView(val elem: Element,
                    items: Rx[SortedSet[RuleFlux]],
                    nodeVisual: KappaNodeVisualSettings,
                    edgeVisual: KappaEdgeVisualSettings,
                    canvas: SVG, containerClass: String = "container") extends BindableView{

  type Node = FluxNode

  type Edge = FluxEdge

  lazy val size: (Double, Double) = elem.getBoundingClientRect() match {
    case rect: ClientRect if rect.width < 100 || rect.height < 100 => (400.0, 400.0)
    case rect: ClientRect => (rect.width, rect.height)
  }

  lazy val width: Var[Double] = Var(size._1)

  lazy val height: Var[Double] = Var(size._2)

  lazy val nodesByName = items.map{ its => its.map{fl=>(fl.rule, new FluxNode(fl))
    }.toMap
  }

  val nodes: Rx[Vector[Node]] = nodesByName.map(nds =>nds.values.toVector)


  lazy val min: Var[Double] = Var(0.0)
  lazy val max: Var[Double] = Var(0.0)

  lazy val edges: Rx[Vector[Edge]] = nodesByName.map{ nds =>
    val params: Vector[(Node, Node, Double)] = nds.flatMap{ case (name, node) => node.flux.flux.collect {
      case (key, value) if value != 0.0 => (node, nds(key), value) }
    }.toVector
    val (minValue, maxValue) = params.foldLeft((0.0, 0.0)){
      case ((mi, ma),(_, _, v)) if v < mi => (v, ma)
      case ((mi, ma),(_, _, v)) if v > ma => (mi, v)
      case (acc, _) => acc
    }
    min() = minValue
    max() = maxValue
    params.map{ case (nodeFrom , nodeTo, value) => new FluxEdge(nodeFrom, nodeTo, value, min, max)}
  }


  lazy val springBase = 100 + nodes.now.length * 10

  val container = elem.selectByClass(containerClass).asInstanceOf[HTMLElement]

  implicit protected def createNodeView(agent: Node): RuleFluxView = {
    new RuleFluxView(agent.flux.rule, nodeVisual.font, nodeVisual.padding, canvas)
    //new KappaAgentView(agent.agent.name, visualSettings.agent.font, visualSettings.agent.padding, visualSettings.canvas)
  }

  protected def lineByValue(value: Double): LineParams = {
    val line = if(value > 0) edgeVisual.line.copy(lineColor = Colors.green) else edgeVisual.line.copy(lineColor = Colors.red)
    line
  }

  implicit protected def createLinkView(edge: Edge): RuleFluxEdgeView = {
    new RuleFluxEdgeView(edge.value.toString, edgeVisual.font, edgeVisual.padding, lineByValue(edge.value), edge.percent, canvas)
  }

  protected def compareRepulsion(node1: Node, node2: Node): (Double, Double) = (node1, node2) match {
    case other => (1, 1)
  }

  protected def computeSpring(edge: Edge) = {
    val p = edge.percent.now
    val length = (1 - p) * springBase + 0.3 * springBase
    dom.console.log(s"${edge.from.label} => ${edge.to.label} VALUE(${edge.value}) PERCENT${p} LENGTH${length} MIN${min.now} MAX${max.now}")
    SpringParams(length, 0.5 + p * 2, 1, 1  )
  }

  protected lazy val gravityForce = new Gravity[Node, Edge](ForceLayoutParams.default3D.gravityMult, ForceLayoutParams.default2D.center)
  protected val repulsionForce = new Repulsion[FluxNode, FluxEdge](ForceLayoutParams.default3D.repulsionMult)(compareRepulsion)
  protected val springForce = new SpringForce[FluxNode, FluxEdge](ForceLayoutParams.default3D.springMult)(computeSpring)
  //protected val borderForce = new BorderForce[FluxNode, FluxEdge](ForceLayoutParams.default2D.repulsionMult / 5, 10, 0.9, ForceLayoutParams.default2D.center)


  protected val forces: Vector[Force[ Node, Edge]] = Vector(
    repulsionForce,
    //attractionForce,
    springForce,
    gravityForce
    //borderForce
  )

  val layouts = Var(Vector(new FluxForceLayout(nodes, edges, ForceLayoutParams.default3D.mode, forces)))

  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    800.0,
    Var(1),
    Var(1)
  )

  def onEdgesChanges(removed: Seq[Edge], added: Seq[Edge]): Unit = {
    for(r <- removed){
        viz.removeSprite(r.view.container)
        //viz.removeObject(r.view.container)
        viz.removeObject(r.line)
    }
    for(a <- added) {
        //viz.addSprite(a.view.container)
        viz.addObject(a.line)
    }
  }

  def onNodesChanges(removed: Seq[Node], added: Seq[Node]): Unit = {
    removed.foreach{ n =>
      //n.view.clearChildren()
      viz.removeSprite(n.view.container)
     // viz.removeObject(n.view.container)
    }
    added.foreach{ n =>
      viz.addSprite(n.view.container)
    }
  }


  protected def subscribeUpdates() = {
    nodes.removedInserted.foreach{
      case (removed, inserted) => onNodesChanges(removed.toList, inserted.toList)
    }
    edges.removedInserted.foreach{case (removed, inserted)=> onEdgesChanges(removed.toList, inserted.toList)}
    layouts.now.foreach(_.start(viz.width, viz.height, viz.camera))

    //bug fix
    onNodesChanges(Seq.empty, nodes.now)
    onEdgesChanges(Seq.empty, edges.now)
  }


  override def bindView() = {
    super.bindView()
    subscribeUpdates()
    viz.render()
  }

}
