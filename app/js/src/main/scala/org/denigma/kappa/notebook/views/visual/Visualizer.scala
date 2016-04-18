package org.denigma.kappa.notebook.views.visual

import org.denigma.binding.extensions.sq
import org.denigma.binding.views.BindableView
import org.denigma.kappa.model.KappaModel
import org.denigma.kappa.model.KappaModel.Side
import org.denigma.threejs.{Object3D, Vector3}
import org.denigma.threejs.extensions.Container3D
import org.denigma.threejs.extensions.controls.JumpCameraControls
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.{Defs, SVG}
import rx._

import scalatags.JsDom
import scalatags.JsDom.{TypedTag, all}
import scalatags.JsDom.svgAttrs
import svgAttrs._
import scalatags.JsDom.svgTags._
import scalatags.JsDom.implicits._



class Visualizer (val container: HTMLElement,
                  val width: Double,
                  val height: Double,
                  val layouts: Rx[Seq[GraphLayout]],
                  agentFontSize: Double,
                  padding: Double,
                  override val distance: Double
                 )
  extends Container3D with Randomizable
{

  override val controls: JumpCameraControls = new  JumpCameraControls(camera, this.container, scene, width, height)

  override def defRandomDistance = distance * 0.6

  def randomPos(obj: Object3D): Vector3 =  obj.position.set(rand(),rand(),rand())

  def onMouseDown(obj: Object3D)(event: MouseEvent ):Unit =  if(event.button==0)
  {
    this.controls.moveTo(obj.position)
  }

  //https://github.com/antonkulaga/semantic-graph/tree/master/graphs/src/main/scala/org/denigma/graphs

  import scalatags.JsDom.all.id

  lazy val s: SVG = {
    val t = svg().render
    t.style.position = "absolute"
    t.style.top = "-9999"
    t
  }
  container.appendChild(s)

  lazy val painter = new AgentPainter(agentFontSize, padding, s)

  def addAgent(agent: KappaModel.Agent) = {
    val txt = text(agent.name, fontSize := agentFontSize)
    //val b: SVGRect = getBox(txt.render)
    val sprite = painter.drawAgent(agent)
    //val sprite = drawBox(agent.name)
    val sp = new HtmlSprite(sprite.render)
    randomPos(sp)
    addSprite(sp)
    sp
  }

  def addSprite(htmlSprite: HtmlSprite) = {
    cssScene.add(htmlSprite)
  }

  override def onEnterFrame() = {
    this.layouts.now.foreach{case l=>
      if(l.active) l.tick()
      //dom.console.info(s"l is ${l.active}")
    }
    super.onEnterFrame()
  }

}

