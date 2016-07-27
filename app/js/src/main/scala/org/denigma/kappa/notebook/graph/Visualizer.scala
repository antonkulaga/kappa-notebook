package org.denigma.kappa.notebook.graph

import org.denigma.binding.extensions._
import org.denigma.kappa.notebook.graph.layouts.GraphLayout
import org.denigma.threejs.extensions.Container3D
import org.denigma.threejs.extensions.controls.JumpCameraControls
import org.denigma.threejs.extras._
import org.denigma.threejs.{Object3D, PerspectiveCamera, WebGLRendererParameters}
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw._
import rx.Ctx.Owner.Unsafe.Unsafe
import rx._

class Visualizer (val container: HTMLElement,
                  val widthRx: Rx[Double],
                  val heightRx: Rx[Double],
                  val layouts: Rx[Vector[GraphLayout]],
                  override val distance: Double,
                  val ticksPerFrame: Var[Int] = Var(5),
                  var ticksOnFirstFrame: Var[Int] = Var(20)
                 )
  extends Container3D
{
  override def width = widthRx.now
  override def height = heightRx.now

  val size = Rx{widthRx() -> heightRx()}
  size.foreach(wh=>resize(wh._1, wh._2))

  protected def resize(w: Double, h:Double) = {
    cssRenderer.setSize(w, h)
    renderer.setSize(w, h)
  }

  override protected def initRenderer= {
    val params = scalajs.js.Dynamic.literal(
      antialias = true,
      alpha = true
      //canvas = container
    ).asInstanceOf[ WebGLRendererParameters]
    import org.denigma.threejs._
    val vr = new WebGLRenderer(params)

    vr.domElement.style.position = "absolute"
    vr.domElement.style.left	  = "0"
    vr.domElement.style.top	  = "0"
    vr.domElement.style.margin	  = "0"
    vr.domElement.style.padding  = "0"
    vr.setSize(width ,height)
    vr
  }

  override protected def initCSSRenderer = {
    val rendererCSS = new HtmlRenderer()
    rendererCSS.setSize(width, height)
    rendererCSS.domElement.style.position = "absolute"
    rendererCSS.domElement.style.left	  = "0"
    rendererCSS.domElement.style.top	  = "0"
    rendererCSS.domElement.style.margin	  = "0"
    rendererCSS.domElement.style.padding  = "0"
    rendererCSS
  }

  val layoutUpdates = layouts.removedInserted

  layoutUpdates.onChange{ case (removed, added)=>
    removed.foreach(r=>r.stop())
    added.foreach(r=>r.start(width, height, camera))
  }

  override val controls: JumpCameraControls = new  JumpCameraControls(camera, this.container, scene, width, height)

  override protected def initCamera() =
  {
    val camera = new PerspectiveCamera(40, this.aspectRatio, 1, 1000)
    camera.position.z = distance
    camera
  }


  def onMouseDown(obj: Object3D)(event: MouseEvent ):Unit =  if(event.button==0)
  {
    this.controls.moveTo(obj.position)
  }

  //https://github.com/antonkulaga/semantic-graph/tree/master/graphs/src/main/scala/org/denigma/graphs
  def addObject(obj: Object3D) = {
    if(cssScene.children.contains(obj)) println("DOUBLE ADDITION OF OBJECT!")
    scene.add(obj)
  }

  def removeObject(obj: Object3D) = {
    scene.remove(obj)
  }


  def addSprite(htmlSprite: Object3D) = {
    if(cssScene.children.contains(htmlSprite)) println("DOUBLE ADDITION OF SPRITE!")
    cssScene.add(htmlSprite)
  }

  def removeSprite(htmlSprite: Object3D) = {
    cssScene.remove(htmlSprite)
  }

  protected def tick() = {
    this.layouts.now.foreach{case l=> l.tick(width, height, camera) }
  }

  override def onEnterFrame() = {
    for(i <- 0 to ticksOnFirstFrame.now) tick()
    ticksOnFirstFrame() = 0
    for(i <- 0 to ticksPerFrame.now) tick()
    super.onEnterFrame()
  }

}

