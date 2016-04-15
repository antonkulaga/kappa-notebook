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

  val font = Var(16.0)

  val padding = Var(10.0)

  val container = sq.byId("graph-container").get

  val viz = new Visualizer(container, defaultWidth, defaultHeight, font.now, padding.now, 600 )

  val agents = List(
    KappaModel.Agent("LacI_RNA"), KappaModel.Agent("LacI"), KappaModel.Agent("LacI_unf"),
    KappaModel.Agent("pLacAra"), KappaModel.Agent("LacI_DNA"), KappaModel.Agent("AraC_DNA"),
    KappaModel.Agent("AraC_RNA"), KappaModel.Agent("AraC"), KappaModel.Agent("AraC_unf")
  )

  for(agent <- agents)
    {
      val ag = viz.addAgent(agent)
    }

  viz.render()

}

/**
  * Created by antonkulaga on 4/10/16.
  */
class Visualizer (val container: HTMLElement,
                  val width: Double,
                  val height: Double,
                  agentFontSize: Double,
                  padding: Double,
                  distance: Double
                 )
  extends Container3D with Randomizable
{
  import scalatags.JsDom.svgAttrs._
  import scalatags.JsDom.svgTags._
  import scalatags.JsDom.implicits._

  override val controls: JumpCameraControls = new  JumpCameraControls(camera, this.container, scene, width, height)

  override def defRandomDistance = distance * 0.6

  type Locatable = SVGElement with SVGLocatable

  def randomPos(obj: Object3D) =  obj.position.set(rand(),rand(),rand())

  def onMouseDown(obj: Object3D)(event: MouseEvent ):Unit =  if(event.button==0)
  {
    this.controls.moveTo(obj.position)
  }

  //https://github.com/antonkulaga/semantic-graph/tree/master/graphs/src/main/scala/org/denigma/graphs

  import scalatags.JsDom.all.id

  lazy val s = {
    val t = svg().render
    t.style.position = "absolute"
    t.style.top = "-9999"
    t
  }
  container.appendChild(s)


  def getBox(e: Locatable) = {
    s.appendChild(e)
    val box = e.getBBox()
    s.removeChild(e)
    box
  }

  val gradient =  defs(
    linearGradient(x1 := 0, x2 := 0, y1 := 0, y2 := "1", scalatags.JsDom.all.id := "GradAgent",
      stop( offset := "0%", stopColor := "skyblue"),
      stop( offset := "50%", stopColor := "deepskyblue"),
      stop( offset := "100%", stopColor := "SteelBlue")
    )
  )

  def addAgent(agent: KappaModel.Agent) = {
    val sprite = drawBox(agent.name)
    val sp = new HtmlSprite(sprite.render)
    randomPos(sp)
    addSprite(sp)
  }

  protected def drawBox(str: String) = {
    val txt = text(str, fontSize := agentFontSize)
    val b = getBox(txt.render)
    val (w: Double, h: Double) = (b.width + padding * 2, b.height + padding * 2)
    val st = 1
    val rec = rect(
      stroke := "blue",
      fill := "url(#GradAgent)",
      strokeWidth := st,
      all.height := h,
      all.width := w,
      rx := 50, ry := 50
    )
    svg(
      all.height := (h + st * 2) , all.width := (w + st * 2),
      gradient, rec, text(str, fontSize := agentFontSize, x:= padding, y := (h - padding))
    )
  }

/*

  def addNode(id:NodeId,data:NodeData, element: HTMLElement, colorName: String):Node =
    this.addNode(id,data, new ViewOfNode(data,new HtmlSprite(element),colorName))


  override def addNode(id: NodeId, data: NodeData, view: ViewOfNode):Node =
  {
    this.randomPos(view.sprite)
    val n = new SimpleNode(data, view)

    view.sprite.element.addEventListener( "mousedown", (this.onMouseDown(view.sprite) _).asInstanceOf[Function[Event,_ ]] )
    cssScene.add(view.sprite)
    this.nodes = nodes + (id->n)
    n
  }




  def addEdge(id:EdgeId,from:SimpleNode,to:SimpleNode, data: EdgeData,element:HTMLElement):Edge =
  {
    val color = Defs.colorMap.get(from.view.colorName) match {
      case Some(c)=>c
      case None=>Defs.color
    }
    val sp = new HtmlSprite(element)
    element.addEventListener( "mousedown", (this.onMouseDown(sp) _).asInstanceOf[Function[Event,_ ]] )
    this.controls.moveTo(sp.position)
    //sp.visible = false

    addEdge(id,from,to,data,new EdgeView(from.view.sprite,to.view.sprite,data,sp, LineParams(color)))

  }

  override def addEdge(id:EdgeId,from:SimpleNode,to:SimpleNode, data: EdgeData,view:ViewOfEdge):Edge =
  {
    cssScene.add(view.sprite)
    val e  = new SimpleEdge(from,to,data,view)
    scene.add(view.arrow)
    edges = edges + (id->e)
    e
  }
*/
  def addSprite(htmlSprite: HtmlSprite) = {
    cssScene.add(htmlSprite)
  }

  override def onEnterFrame() = {
    super.onEnterFrame()
    /*
    his.layouts.foreach{case l=>
      if(l.active) l.tick()
      //dom.console.info(s"l is ${l.active}")
    }
    */
  }

}

