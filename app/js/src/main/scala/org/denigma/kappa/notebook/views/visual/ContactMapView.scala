package org.denigma.kappa.notebook.views.visual

import org.denigma.binding.views.BindableView
import org.denigma.kappa.messages.{KappaMessage, ServerMessages, WebSimMessages}
import org.scalajs.dom.raw.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe
import org.denigma.binding.extensions._
import org.denigma.kappa.messages.KappaMessage.ServerResponse

import scalajs.js.JSConverters._
import scala.collection.immutable.List
import scala.scalajs.js
import scala.scalajs.js.Array
import scala.scalajs.js.annotation.{JSName, ScalaJSDefined}
/*
@JSName("ContactMap")
object ContactMapJS {
  implicit def fromContactMap(cm: WebSimMessages.ContactMap): ContactMapJS = {
    val nodes: Array[WebSimNodeJS] = js.Array(cm.contact_map.map(n=>n:WebSimNodeJS):_*)
    new ContactMapJS(nodes)
  }
}

@ScalaJSDefined
class ContactMapJS(val contact_map: js.Array[WebSimNodeJS]) extends js.Object
*/
object WebSimNodeJS {
  implicit def fromWebSimNode(node: WebSimMessages.WebSimNode): WebSimNodeJS = {
    val sites = js.Array(node.node_sites.map(s=>s: WebSimSideJS):_*)
    new WebSimNodeJS(node.node_name, sites)
  }
}

@JSName("Node")
@ScalaJSDefined
class WebSimNodeJS(val node_name: String, val node_sites: js.Array[WebSimSideJS]) extends js.Object{

}

object WebSimSideJS {
  implicit def fromWebSimSide(site: WebSimMessages.WebSimSide): WebSimSideJS = {
    val links = js.Array(site.site_links.map{
      case (from, to)=> js.Array(from, to)
    }:_*)
    val states: js.Array[String] = js.Array(site.site_states:_*)
    new WebSimSideJS(site.site_name, links, states)
  }
}

@ScalaJSDefined
class WebSimSideJS(val site_name: String, val site_links: js.Array[js.Array[Int]], val site_states: js.Array[String] ) extends js.Object


@js.native
@JSName("ContactMap")
class ContactMapRenderer(val id: String, isSnapshot: Boolean) extends js.Object {

  def setData(data: js.Array[WebSimNodeJS]): Unit = js.native

  def clearData(): Unit = js.native

  def exportJSON: Any = js.native

}


class ContactMapView(val elem: Element,  val input: Var[KappaMessage], val active: Rx[Boolean]) extends BindableView {

  val contactMap: Var[WebSimMessages.ContactMap] = Var(WebSimMessages.ContactMap.empty)
  import scala.concurrent.duration._

  Rx{
    val cm = contactMap()
    val act = active()
    if(act) js.timers.setTimeout(300 millis)
    {
      val nodes: js.Array[WebSimNodeJS] = js.Array(cm.contact_map.map(n=>n:WebSimNodeJS):_*)
      renderer.setData(nodes)
    }
  }

  input.onChange{

    case KappaMessage.ServerResponse(server, ServerMessages.ParseResult(cmap))=>
      contactMap() = cmap

    //case KappaMessage.ServerResponse(ServerMessages.LaunchModel) =>
    case _ => //do nothing

  }

  lazy val renderer = new ContactMapRenderer(elem.id, false)

}
