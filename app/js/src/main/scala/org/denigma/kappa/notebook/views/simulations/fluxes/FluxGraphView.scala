package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
import org.denigma.kappa.notebook.graph.layouts._
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all.attrs._
import org.scalajs.dom.raw.{ClientRect, Element, HTMLElement}
import org.scalajs.dom.svg.{LinearGradient, SVG}

import scala.collection.immutable._



class RuleFluxEdgeView(val label: String, val fontSize: Double, val padding: Double, lines: LineParams, val s: SVG) extends  KappaView
{

  override def gradientName: String =  "RuleFluxEdge"

  protected lazy val defaultGradient =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )

  lazy val gradient = Var(defaultGradient)

}

import scalatags.JsDom.TypedTag
class RuleFluxView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends KappaView {
  override def gradientName: String = "RuleFluxView"

  protected lazy val defaultGradient: TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )

  lazy val gradient = Var(defaultGradient)
}

class FluxNode(val flux: RuleFlux)(implicit val create: FluxNode => RuleFluxView ) extends KappaNode {
  val view = create(this)
  val layoutInfo: LayoutInfo = new LayoutInfo(1)

}

class FluxEdge(val from: FluxNode, val to: FluxNode, val value: Double, val lineParams: LineParams)(implicit val create: FluxEdge => RuleFluxEdgeView) extends ArrowEdge{

  override type FromNode = FluxNode
  override type ToNode = FluxNode

  val view = create(this)

  override def update() = {
    posArrow()
    posSprite()
  }

  def posSprite() = {
    val m = middle
    view.container.position.set(m.x, m.y, m.z)
  }

}

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

  val width: Var[Double] = Var(size._1)

  val height: Var[Double] = Var(size._2)

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
    new RuleFluxEdgeView(edge.value.toString, edgeVisual.font, edgeVisual.padding, lineByValue(edge.value), canvas)
  }

  protected def compareRepulsion(node1: Node, node2: Node): (Double, Double) = (node1, node2) match {
    case other => (1, 1)
  }

  val nodesByName = items.map{
    case its => its.map(fl=>(fl.rule, new FluxNode(fl))).toMap
  }

  val nodes: Rx[Vector[Node]] = nodesByName.map(nds =>nds.values.toVector)

  val edges: Rx[Vector[Edge]] = nodesByName.map{ case nds => nds.flatMap{
      case (name, node) => node.flux.flux.collect {
        case (key, value) if value != 0.0 =>

          new FluxEdge(node, nds(key), value, edgeVisual.line)
      }
    }.toVector
  }

  val min = edges.map(e => e.minBy(_.value).value)
  val max = edges.map(e => e.maxBy(_.value).value)

  protected def percent(value: Double): Double = (value , max.now, min.now) match {
    case (v, m, _) if v > 0 => v / m
    case (v, _, m) if v < 0 => v / m
    case _ => 0.0
  }

  protected def computeSpring(edge: Edge) = {
    val p = percent(edge.value)
    val length = (1 - p) * 50 + 10
    SpringParams(length, 0.5 + p * 2, 1, 1  )
  }

  //protected val gravityForce = new Gravity[FluxNode, FluxEdge](ForceLayoutParams.default2D.springMult / 4, ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)
  protected val repulsionForce = new Repulsion[FluxNode, FluxEdge](ForceLayoutParams.default2D.repulsionMult)(compareRepulsion)
  protected val springForce = new SpringForce[FluxNode, FluxEdge](ForceLayoutParams.default2D.springMult)(computeSpring)
  //protected val borderForce = new BorderForce[FluxNode, FluxEdge](ForceLayoutParams.default2D.repulsionMult / 5, 10, 0.9, ForceLayoutParams.default2D.center)


  protected val forces: Vector[Force[ Node, Edge]] = Vector(
    repulsionForce,
    //attractionForce,
    springForce
    //gravityForc
    //borderForce
  )

  val layouts = Var(Vector(new FluxForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces)))

  val viz = new Visualizer(container,
    width,
    height,
    layouts,
    1000.0
  )

  def onEdgesChanges(removed: Seq[Edge], added: Seq[Edge]): Unit = {
    for(r <- removed){
        viz.removeSprite(r.view.container)
        viz.removeObject(r.view.container)
        viz.removeObject(r.arrow)
    }
    for(a <- added) {
        viz.addSprite(a.view.container)
        viz.addObject(a.arrow)
    }
  }

  def onNodesChanges(removed: Seq[Node], added: Seq[Node]): Unit = {
    //removed.foreach(r=> dom.console.log("REMOVED NODE "+r.view.label))
    //added.foreach(a=> dom.console.log("ADDED NODE "+a.view.label))

    removed.foreach{ case n =>
      n.view.clearChildren()
      viz.removeSprite(n.view.container)
      viz.removeObject(n.view.container)
    }
    added.foreach(n => viz.addSprite(n.view.container))
    //println("REMOVED = "+removed)
    //println("INSERTED = "+added)
  }


  protected def subscribeUpdates() = {
    nodes.foreach{
     case nds =>
       println("NODES CHANGE")
       nds.foreach(n=>println("node is "+ n))
    }
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
