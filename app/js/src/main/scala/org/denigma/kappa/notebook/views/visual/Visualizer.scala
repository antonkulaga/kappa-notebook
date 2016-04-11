package org.denigma.kappa.notebook.views.visual

import org.denigma.binding.extensions.sq
import org.denigma.binding.views.BindableView
import org.denigma.threejs.Object3D
import org.denigma.threejs.extensions.Container3D
import org.denigma.threejs.extensions.controls.JumpCameraControls
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.{Element, HTMLElement}
import rx._
import scalatags.JsDom.all._

class GraphView(val elem: Element) extends BindableView
{

  val active: Var[Boolean] = Var(false)


  /*
  protected def defaultWidth = elem.getBoundingClientRect().width

  protected def defaultHeight = 200


  val container = sq.byId("graph-container").get
  val viz = new Visualizer(container, defaultWidth, defaultHeight )


  for(i <- 1 to 10)
    {
      val tag = label( `class` := "ui teal label", "Hello World #"+i)
      val sp = new HtmlSprite(tag.render)
      viz.randomPos(sp)
      viz.addSprite(sp)
    }

  viz.render()
  */
}

/**
  * Created by antonkulaga on 4/10/16.
  */
class Visualizer (val container: HTMLElement,
                  val width: Double,
                  val height: Double,
                  override val distance:Double = 500
                 )
  extends  Container3D with Randomizable
{

  override val controls: JumpCameraControls = new  JumpCameraControls(camera, this.container, scene, this.width, this.height)

  override def defRandomDistance = distance * 0.6

  def randomPos(obj: Object3D) =  obj.position.set(rand(),rand(),rand())

  def onMouseDown(obj: Object3D)( event: MouseEvent ):Unit =  if(event.button==0)
  {
    this.controls.moveTo(obj.position)
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

