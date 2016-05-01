package org.denigma.kappa.notebook.views.visual

import org.denigma.kappa.model.KappaModel
import org.denigma.threejs.extensions.Container3D
import org.denigma.threejs.extensions.controls.JumpCameraControls
import org.denigma.threejs.{Object3D, PerspectiveCamera, Vector3, WebGLRendererParameters}
import org.denigma.threejs.extras._
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.SVG
import rx._

import scalatags.JsDom.implicits._
import scalatags.JsDom.svgAttrs._
import scalatags.JsDom.svgTags._
import org.denigma.binding.extensions._


class Visualizer (val container: HTMLElement,
                  val width: Double,
                  val height: Double,
                  val layouts: Rx[Vector[GraphLayout]],
                  override val distance: Double
                 )
  extends Container3D
{

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
    vr.setSize(width,height)
    vr
  }

  override protected def initCSSRenderer = {
    val rendererCSS = new HtmlRenderer()
    rendererCSS.setSize(width,height)
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

  override def onEnterFrame() = {
    this.layouts.now.foreach{case l=>
      if(l.active) l.tick(width, height, camera)
      //dom.console.info(s"l is ${l.active}")
    }
    super.onEnterFrame()
  }

}

