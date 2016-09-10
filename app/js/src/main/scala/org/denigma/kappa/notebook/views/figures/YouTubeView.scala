package org.denigma.kappa.notebook.views.figures

import im.conversant.apps.youtube.{PlayerEvents, PlayerVars, Player, PlayerOptions}
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import scalatags.JsDom.all
import scalatags.JsDom.all._
import scalajs.js
import org.scalajs.dom.ext._



class YouTubeView(val elem: Element, val selected: Var[String], val video: Var[Video]) extends FigureView {


  lazy val videoID =video.map(v=>YouTubeView.idFromURL(v.url)) //"HFwSnqYC5LA"

  lazy val text = video.map(v=>v.text)

  lazy val hasText = video.map(vid=>vid.text!="")

  def onPlayerReady(event:  im.conversant.apps.youtube.Event): Any = {
    println("on play ready")
  }

  def onPlayerError(event: im.conversant.apps.youtube.Event): Any = {
    println("on play error")
  }

  def onPlayerStateChange(event: im.conversant.apps.youtube.Event): Any = {
    println("onPlayerStateChange")
  }

  lazy val playerEvents = {
    PlayerEvents(onPlayerReady _, onPlayerError _, onPlayerStateChange _)
  }

  lazy val figureId = this.id + "_figure"

  override def bindView() = {
    if(!elem.children.exists(e=>e.id == figureId)) {
      val dataKey = attr("data-bind-src")
      val child = div(all.id := figureId)
      elem.appendChild(child.render)
    }
    super.bindView()
    initPlayer(figureId)
  }

  def initPlayer(ident: String) = {
    YouTubeView.activateAPI().foreach{ _=>
        val player = new Player(ident, PlayerOptions(
          width = "100%",
          //height = "100%",
          videoId = videoID.now,
          events =  playerEvents,
          playerVars = PlayerVars(
            playsinline = 1.0
          )
        ))
    }
  }
}


import rx.Var
import org.denigma.binding.extensions._
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object YouTubeView {

  def idFromURL(str: String): String = Video.shorterURL(str)

  val activated = Var(false)

  def activateAPI(): Future[Unit] = {
    sq.byId("youtubeapi") match {
      case None=>
        var tag = org.scalajs.dom.document.createElement("script").asInstanceOf[org.scalajs.dom.html.Script]
        tag.id = "youtubeapi"
        tag.src = "https://www.youtube.com/iframe_api"
        var firstScriptTag = org.scalajs.dom.document.getElementsByTagName("script").item(0)
        firstScriptTag.parentNode.insertBefore(tag, firstScriptTag)
      case other =>
    }
    activate()
  }

  protected def activate() = {
    val p = Promise[Unit]
    if(activated.now) p.success(Unit) else
      org.scalajs.dom.window.asInstanceOf[js.Dynamic].onYouTubeIframeAPIReady = () => {
        println("activation happens")
        activated() = true
        p.success(Unit)
      }
    p.future
  }
}
