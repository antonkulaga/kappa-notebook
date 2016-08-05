package org.denigma.kappa.notebook.views.visual.rules

import org.denigma.binding.views.BindableView
import org.denigma.kappa.notebook.graph.Change.Change
import org.denigma.kappa.notebook.graph._
import org.denigma.kappa.notebook.graph.layouts._
import org.scalajs.dom.svg.LinearGradient
import rx._

import scala.Vector
import scala.collection.immutable._
import scalatags.JsDom.TypedTag
import rx.Ctx.Owner.Unsafe.Unsafe


case class Gradient(name: String, change: Change, tag: TypedTag[LinearGradient])

object Gradients {
  import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all._
  import org.denigma.kappa.notebook.graph.drawing.SvgBundle.all.attrs._

  def blueGradient(gradientName: String): TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue"),
      stop(offset := "50%", stopColor := "deepskyblue"),
      stop(offset := "100%", stopColor := "SteelBlue")
    )

  def opaqueBlueGradient(gradientName: String): TypedTag[LinearGradient] =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "skyblue", stopOpacity := "0.3"),
      stop(offset := "50%", stopColor := "deepskyblue", stopOpacity := "0.3"),
      stop(offset := "100%", stopColor := "SteelBlue", stopOpacity := "0.3")
    )

  def lightBlueGradient(gradientName: String): TypedTag[LinearGradient]  =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "white"),
      stop(offset := "100%", stopColor := "deepskyblue")
    )


  def superLightBlueGradient(gradientName: String): TypedTag[LinearGradient]  =
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#e6f2ff"),
      stop(offset := "100%", stopColor := "#cce5ff")
    )

  /*
    <svg height="52.171875" width="88.796875">
    <defs>
    <linearGradient x1="0" x2="0" y1="0" y2="1" id="GradAgent">
      <stop offset="0%" stop-color="#adebad"></stop>
      <stop offset="50%" stop-color="#40bf40"></stop>
      <stop offset="100%" stop-color="#609f60"></stop>
    </linearGradient>
    </defs>
    <svg height="50.171875" width="86.796875" x="0" y="0">
    <rect stroke="blue" fill="url(#GradAgent)" stroke-width="2" height="48.171875" width="86.796875" rx="20" ry="20"></rect>
    <text font-size="32" x="12" y="44.171875">TetR</text>
    </svg>
    </svg>
  */

  def redGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#ff9999"),
      stop(offset := "50%", stopColor := "#ff6666"),
      stop(offset := "100%", stopColor := "#ff6666")
    )
  }

  def opaqueRedGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#ff9999", stopOpacity := "0.3"),
      stop(offset := "50%", stopColor := "#ff6666", stopOpacity := "0.3"),
      stop(offset := "100%", stopColor := "#ff6666", stopOpacity := "0.3")
    )
  }


  def lightRedGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#ff9999"),
      stop(offset := "50%", stopColor := "#ff5050"),
      stop(offset := "100%", stopColor := "#ff1a1a")
    )
  }

  def superLightRedGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#ffe6e6"),
      stop(offset := "100%", stopColor := "#ffcccc")
    )
  }

  def opaqueGreenGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#adebad", stopOpacity := "0.3"),
      stop(offset := "50%", stopColor := "#40bf40", stopOpacity := "0.3"),
      stop(offset := "100%", stopColor := "#609f60", stopOpacity := "0.3")
    )
  }

  def greenGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#adebad"),
      stop(offset := "50%", stopColor := "#40bf40"),
      stop(offset := "100%", stopColor := "#609f60")
    )
  }

  def lightGreenGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#b3ffb3"),
      stop(offset := "50%", stopColor := "#00e600"),
      stop(offset := "100%", stopColor := "#009900")
    )
  }


  def superLightGreenGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#e6ffe6"),
      stop(offset := "100%", stopColor := "#ccffcc")
    )
  }

  def purpleGradient(gradientName: String) = {
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := gradientName,
      stop(offset := "0%", stopColor := "#e6b3ff"),
      stop(offset := "50%", stopColor := "#d580ff"),
      stop(offset := "100%", stopColor := "#b31aff")
    )
  }

  /*
  def gradientByStatus(status: Change.Change, gradientName: String) = status match {
    case Change.Removed => Gradients.redGradient(gradientName)
    case Change.Added => Gradients.greenGradient(gradientName)
    case Change.Unchanged | Change.Updated =>
      if(gradientName == KappaSiteView.gradientName)  Gradients.lightBlueGradient(gradientName) else Gradients.blueGradient(gradientName)
    //case Change.Updated => if(gradientName == KappaSiteView.gradientName)  Gradients.purpleGradient(gradientName) else Gradients.purpleGradient(gradientName)

  }*/
}

trait VisualGraph extends BindableView {

  type Node = KappaNode

  type Edge = ChangeableEdge

  def visualSettings: RulesVisualSettings

  protected val gradients =  Var(Map.empty[(String, Change.Change), TypedTag[LinearGradient]])

  protected def addGradient(name: String, status: Change.Change, tag: TypedTag[LinearGradient]) = {
    gradients() = gradients.now.updated((name, status), tag)
  }

  def getGradient(gradientName: String, status: Change.Change) = {
    gradients.now.getOrElse((gradientName, status), Gradients.blueGradient(gradientName))
  }

  def fillGradients() = {
    addGradient(KappaAgentView.gradientName, Change.Removed, Gradients.redGradient(KappaAgentView.gradientName))
    addGradient(KappaSiteView.gradientName, Change.Removed, Gradients.lightRedGradient(KappaSiteView.gradientName))
    addGradient(KappaStateView.gradientName, Change.Removed, Gradients.superLightRedGradient(KappaStateView.gradientName))
    addGradient(KappaLinkView.gradientName, Change.Removed, Gradients.opaqueRedGradient(KappaLinkView.gradientName))

    addGradient(KappaAgentView.gradientName, Change.Added, Gradients.greenGradient(KappaAgentView.gradientName))
    addGradient(KappaSiteView.gradientName, Change.Added, Gradients.lightGreenGradient(KappaSiteView.gradientName))
    addGradient(KappaStateView.gradientName, Change.Added, Gradients.superLightGreenGradient(KappaStateView.gradientName))
    addGradient(KappaLinkView.gradientName, Change.Added, Gradients.opaqueGreenGradient(KappaLinkView.gradientName))

    addGradient(KappaAgentView.gradientName, Change.Unchanged, Gradients.blueGradient(KappaAgentView.gradientName))
    addGradient(KappaSiteView.gradientName, Change.Unchanged, Gradients.lightBlueGradient(KappaSiteView.gradientName))
    addGradient(KappaStateView.gradientName, Change.Unchanged, Gradients.superLightBlueGradient(KappaStateView.gradientName))
    addGradient(KappaLinkView.gradientName, Change.Unchanged, Gradients.opaqueBlueGradient(KappaLinkView.gradientName))

    addGradient(KappaAgentView.gradientName, Change.Updated, Gradients.blueGradient(KappaAgentView.gradientName))
    addGradient(KappaSiteView.gradientName, Change.Updated, Gradients.lightBlueGradient(KappaSiteView.gradientName))
    addGradient(KappaStateView.gradientName, Change.Updated, Gradients.superLightBlueGradient(KappaStateView.gradientName))
    addGradient(KappaLinkView.gradientName, Change.Updated, Gradients.opaqueBlueGradient(KappaLinkView.gradientName))
  }
  fillGradients()

  implicit protected def createAgentNodeView(agent: AgentNode): KappaAgentView = {
    val gradient = getGradient(KappaAgentView.gradientName, agent.status)
    new KappaAgentView(agent.agent.name, visualSettings.agent.font, visualSettings.agent.padding, gradient, visualSettings.canvas)
  }

  implicit protected def createSiteNodeView(site: SiteNode): KappaSiteView = {
    val gradient = getGradient(KappaSiteView.gradientName, site.status)
    new KappaSiteView(site.site.name, visualSettings.sites.font, visualSettings.sites.padding, gradient, visualSettings.canvas)
  }

  implicit protected def createStateNodeView(state: StateNode): KappaStateView = {
    val gradient = getGradient(KappaStateView.gradientName, state.status)
    new KappaStateView(state.state.name, visualSettings.state.font, visualSettings.state.padding, gradient, visualSettings.canvas)
  }

  implicit protected def createLinkView(edge: KappaLinkEdge): KappaLinkView = {
    val gradient = getGradient(KappaLinkView.gradientName, edge.status)
    new KappaLinkView(edge.link.label, visualSettings.link.font, visualSettings.link.padding, gradient, visualSettings.canvas)
  }

}


trait  RuleGraphWithForces extends VisualGraph{

  lazy val minSpring = 75

  def massByNode(node: KappaNode): Double = node match {
    case n: AgentNode => 1.4
    case s: SiteNode => 1.0
    case st: StateNode => 0.8
  }

  protected def computeSpring(edge: Edge): SpringParams = (edge.from, edge.to) match {
    case (from: SiteNode, to: SiteNode) => SpringParams(minSpring * 1.4, 1.4, massByNode(from), massByNode(to))
    case (from: SiteNode, to: AgentNode) => SpringParams(minSpring, 1, massByNode(from), massByNode(to))
    case (from: AgentNode, to: SiteNode) => SpringParams(minSpring, 1, massByNode(from), massByNode(to))
    case (from: AgentNode, to: AgentNode) => SpringParams(minSpring, 2, massByNode(from), massByNode(to))
    case (from: KappaNode, to: KappaNode) => SpringParams(minSpring, 1, massByNode(from), massByNode(to))
  }

  protected lazy val forces: Vector[Force[ Node, Edge]] = Vector(
    repulsionForce,
    springForce,
    gravityForce
    //,borderForce
  )
  protected val gravityForce = new Gravity[Node, Edge](ForceLayoutParams.default2D.gravityMult, ForceLayoutParams.default2D.center)
  protected lazy val repulsionForce: Repulsion[Node, Edge] = new Repulsion[Node, Edge](ForceLayoutParams.default2D.repulsionMult)(compareRepulsion)
  protected lazy val springForce: SpringForce[Node, Edge] = new SpringForce[Node, Edge](ForceLayoutParams.default2D.springMult)(computeSpring)
  protected lazy val borderForce: BorderForce[Node, Edge] = new BorderForce[Node, Edge](ForceLayoutParams.default2D.repulsionMult / 5, 10, 0.9, ForceLayoutParams.default2D.center)

  protected def compareRepulsion(node1: Node, node2: Node): (Double, Double) = (massByNode(node1), massByNode(node2))


  lazy val iterationsPerFrame = Var(10)
  lazy val firstFrameIterations = Var(300)

  def layouts: Var[Vector[GraphLayout]]
}