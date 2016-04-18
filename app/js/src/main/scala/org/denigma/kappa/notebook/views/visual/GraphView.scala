package org.denigma.kappa.notebook.views.visual


import org.denigma.binding.extensions.sq
import org.denigma.binding.views.BindableView
import org.denigma.kappa.model.KappaModel
import org.denigma.threejs.Object3D
import org.denigma.threejs.extensions.Container3D
import org.denigma.threejs.extensions.controls.JumpCameraControls
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.{Element, HTMLElement, SVGElement, SVGLocatable}
import rx._
import scalatags.JsDom.all

class GraphView(val elem: Element) extends BindableView
{

  val active: Var[Boolean] = Var(true)// Var(false)

  protected def defaultWidth = elem.getBoundingClientRect().width

  protected def defaultHeight: Double = Math.max(250.0, dom.window.innerHeight / 4)

  val font = Var(20.0)

  val padding = Var(10.0)

  val container = sq.byId("graph-container").get


  val nodes: Var[Vector[KappaNode]] = Var(Vector.empty)

  val edges: Var[Vector[KappaEdge]] = Var(Vector.empty)

  val layouts = Var(Seq(new ForceLayout(nodes, edges, defaultWidth, defaultHeight, ForceLayoutParams.default3D)))

  val viz = new Visualizer(container,
    defaultWidth,
    defaultHeight,
    layouts,
    font.now,
    padding.now, 800.0
  )

  import KappaModel._

  val agents = List(
    KappaModel.Agent("LacI_RNA"),
    KappaModel.Agent("LacI", List( Side("left"), Side("right"), Side("dna") )),
    KappaModel.Agent("LacI_unf"),
    KappaModel.Agent("pLacAra", List( Side("araC"), Side("lacI1"), Side("lacI2"), Side("down") )),
    KappaModel.Agent("LacI_DNA", List(Side("up"))),
    KappaModel.Agent("AraC_DNA", List(Side("up"))),
    KappaModel.Agent("AraC_RNA"),
    KappaModel.Agent("AraC", List( Side("ara"), Side("dna") )),
    KappaModel.Agent("AraC_unf")
  )

  for(agent <- agents)
    {
      val view = viz.addAgent(agent)
      val node = new KappaNode(agent, view)
      nodes() = nodes.now :+ node
    }

  viz.render()
  layouts.now.foreach(_.start())

}
