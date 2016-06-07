package org.denigma.kappa.notebook.views.annotations

import org.denigma.nlp.annotator.AnnotatorView
import org.denigma.nlp.communication.WebSocketNLPTransport
import org.scalajs.dom.Element
import rx._
/*
case class NLPService(host: String, channel: String, username: String) extends WebSocketNLPTransport() {
  override def getWebSocketUri(username: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/channel/$channel?username=$username"
  }
}
*/

class AnnotatorNLP(elem: Element, connector: WebSocketNLPTransport) extends AnnotatorView(elem: Element, connector: WebSocketNLPTransport){

    val server: Var[String] = Var(s"${connector.protocol}://${connector.host}/channel/${connector.channel}")

}
