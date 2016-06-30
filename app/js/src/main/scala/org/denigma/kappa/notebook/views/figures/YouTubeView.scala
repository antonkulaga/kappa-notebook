package org.denigma.kappa.notebook.views.figures

import im.conversant.apps.youtube.{PlayerEvents, PlayerVars, Player, PlayerOptions}
import org.scalajs.dom
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import scalajs.concurrent.JSExecutionContext.Implicits.queue



class YouTubeView(val elem: Element, val selected: Var[String], val video: Var[Video]) extends FigureView {


 lazy val videoID =video.map{
   case v if v.url.contains(YouTubeView.WATCH) =>
     val i = v.url.indexOf(YouTubeView.WATCH)
     v.url.substring(i+YouTubeView.WATCH.length)

   case v => v.url
 } //"HFwSnqYC5LA"

  override def update(value: Figure) =  value match {
    case v @ Video(name, url)=>
      video() = v
      this

    case other => dom.console.error("not a valid Video Item")
      this
  }

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

  override def bindView() = {
    super.bindView()
    YouTubeView.activateAPI().foreach{
      case _=>
        println("initiating player")
        val player = new Player("player", PlayerOptions(
          //width = "100%",
          //height = "100%",
          videoId = videoID.now, //url is shortened to id
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

  val WATCH = "watch?v="

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
