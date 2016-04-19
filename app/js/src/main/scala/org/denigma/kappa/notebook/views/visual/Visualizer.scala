package org.denigma.kappa.notebook.views.visual

import org.denigma.kappa.model.KappaModel
import org.denigma.threejs.extensions.Container3D
import org.denigma.threejs.extensions.controls.JumpCameraControls
import org.denigma.threejs.{Object3D, Vector3}
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
  extends Container3D with Randomizable
{

  val layoutUpdates = layouts.removedInserted

  layoutUpdates.onChange{ case (removed, added)=>
    removed.foreach(r=>r.stop())
    added.foreach(r=>r.start(width, height, camera))
  }

  override def defRandomDistance = distance * 0.6

  override val controls: JumpCameraControls = new  JumpCameraControls(camera, this.container, scene, width, height)

  def randomPos(obj: Object3D): Vector3 =  obj.position.set(rand(),rand(),rand())

  def onMouseDown(obj: Object3D)(event: MouseEvent ):Unit =  if(event.button==0)
  {
    this.controls.moveTo(obj.position)
  }

  //https://github.com/antonkulaga/semantic-graph/tree/master/graphs/src/main/scala/org/denigma/graphs
  def addObject(obj: Object3D) = {
    scene.add(obj)
  }

  def removeObject(obj: Object3D) = {
    scene.add(obj)
  }


  def addSprite(htmlSprite: HtmlSprite) = {
    randomPos(htmlSprite)
    cssScene.add(htmlSprite)
  }

  def removeSprite(htmlSprite: HtmlSprite) = {
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

