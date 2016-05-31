package org.denigma.kappa.notebook.views.annotations

import org.denigma.kappa.notebook.WebSocketTransport
import org.denigma.nlp.annotator.AnnotatorView
import org.denigma.nlp.communication.WebSocketNLPTransport
import org.scalajs.dom
import org.scalajs.dom.Element
/*
case class NLPService(host: String, channel: String, username: String) extends WebSocketNLPTransport() {
  override def getWebSocketUri(username: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/channel/$channel?username=$username"
  }
}
*/

class NLP(elem: Element, connector: WebSocketNLPTransport) extends AnnotatorView(elem: Element, connector: WebSocketNLPTransport){

    import org.scalajs.dom.raw.Element

}
