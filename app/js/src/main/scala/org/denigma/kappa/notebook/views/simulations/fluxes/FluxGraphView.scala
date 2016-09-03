package org.denigma.kappa.notebook.views.simulations.fluxes

import org.denigma.binding.extensions._
import org.denigma.binding.views.BindableView
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.drawing.{KappaPainter, Rectangle}
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
import org.denigma.kappa.notebook.graph.layouts._
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all.attrs._
import org.denigma.threejs.extras.HtmlSprite
import org.scalajs.dom
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



trait SimpleKappaView extends KappaPainter
{
  def label: String

  def fontSize: Double
  def padding: Double
  protected def gradientName: String
  def gradient:  Var[TypedTag[LinearGradient]]

  lazy val textBox = getTextBox(label, fontSize)

  lazy val labelBox = textBox.withPadding(padding, padding)

  def opacity = sprite.now.element.style.opacity
  def opacity_=(value: Double) = {
    this.sprite.now.element.style.opacity = value.toString
  }

  protected val svg = Rx {
    labelStrokeColor()
    val st = labelStrokeWidth()
    val grad = gradient()
    val rect = Rectangle(textBox.width, textBox.height).withPadding(padding * 2, padding)
    val lb: TypedTag[SVG] = drawLabel(label, rect, textBox, fontSize, gradientName)
    drawSVG(rect.width + st *2, rect.height + st *2 , List(gradient.now), List(lb))
  }

  lazy val sprite: Rx[HtmlSprite] = svg.map {
    s =>
      val sp = new HtmlSprite(s.render)
      sp
  }
  def container = sprite.now
  sprite.zip.onChange{ case (prev, cur) =>
      if(prev!=cur) dom.console.log(s"chage of sprite to ${cur.element.outerHTML}")
    /*
      if(prev!=cur) prev.parent match {
        case p if p!=null && !scalajs.js.isUndefined(p) =>
          p.remove(prev)
          p.add(cur)
        case _ =>
      }
      */
  }
}

class RuleFluxView(val label: String, val fontSize: Double, val padding: Double, val s: SVG) extends SimpleKappaView {
  override def gradientName: String = "RuleFluxView"

  protected lazy val defaultGradient: TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )

  lazy val gradient = Var(defaultGradient)
}

class FluxNode(val flux: RuleFlux)(implicit val create: FluxNode => RuleFluxView ) extends ForceNode {
  val view = create(this)
  val layoutInfo: LayoutInfo = new LayoutInfo(1)
  override def position = view.container.position

}

class FluxEdge(val from: FluxNode, val to: FluxNode, val value: Double)(implicit val create: FluxEdge => RuleFluxEdgeView) extends LineEdge{
  self =>

  override type FromNode = FluxNode
  override type ToNode = FluxNode

  val view = create(this)

  lazy val color = if(Math.abs(value) < 1) { Colors.blue
  } else if(value> 0) Colors.green else Colors.red

  lazy val lineParams = LineParams(lineColor = self.color, thickness = 2)

  override def update() = {
    posLine()
    posSprite()
    opacity =  if(Math.abs(value) < 1) 0.5 else 1.0
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

  lazy val width: Var[Double] = Var(size._1)

  lazy val height: Var[Double] = Var(size._2)

  lazy val nodesByName = items.map{ its => its.map(fl=>(fl.rule, new FluxNode(fl))).toMap }

  val nodes: Rx[Vector[Node]] = nodesByName.map(nds =>nds.values.toVector)

  val edges: Rx[Vector[Edge]] = nodesByName.map{ nds => nds.flatMap{
    case (name, node) => node.flux.flux.collect {
      case (key, value) if value != 0.0 =>
        new FluxEdge(node, nds(key), value)
    }
  }.toVector
  }


  lazy val springBase = 125 + nodes.now.length * 10

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

  val min = edges.map(e => e.minBy(_.value).value)
  val max = edges.map(e => e.maxBy(_.value).value)

  protected def percent(value: Double): Double = (value, max.now, min.now) match {
    case (v, mx, _) if v > 0 => v / mx
    case (v, _, mi) if v < 0 => v / mi
    case _ => 0.0
  }

  protected def computeSpring(edge: Edge) = {
    val p = percent(edge.value)
    val length = (1 - p) * springBase + 0.1 * springBase
    SpringParams(length, 0.5 + p * 2, 1, 1  )
  }

  protected lazy val gravityForce = new Gravity[Node, Edge](ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)
  protected val repulsionForce = new Repulsion[FluxNode, FluxEdge](ForceLayoutParams.default2D.repulsionMult / 4)(compareRepulsion)
  protected val springForce = new SpringForce[FluxNode, FluxEdge](ForceLayoutParams.default2D.springMult)(computeSpring)
  //protected val borderForce = new BorderForce[FluxNode, FluxEdge](ForceLayoutParams.default2D.repulsionMult / 5, 10, 0.9, ForceLayoutParams.default2D.center)


  protected val forces: Vector[Force[ Node, Edge]] = Vector(
    repulsionForce,
    //attractionForce,
    springForce ,
    gravityForce
    //borderForce
  )

  val layouts = Var(Vector(new FluxForceLayout(nodes, edges, ForceLayoutParams.default2D.mode, forces)))

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
