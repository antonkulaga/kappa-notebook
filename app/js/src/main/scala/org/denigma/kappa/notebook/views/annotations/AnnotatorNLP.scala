package org.denigma.kappa.notebook.views.annotations
/*
import org.denigma.nlp.annotator.AnnotatorView
import org.denigma.nlp.communication.WebSocketNLPTransport
import org.scalajs.dom.Element
import rx._
import rx.Ctx.Owner.Unsafe.Unsafe


case class NLPService(host: String, channel: String, username: String) extends WebSocketNLPTransport() {
  override def getWebSocketUri(username: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/channel/$channel?username=$username"
  }
}
*/
/*
class AnnotatorNLP(elem: Element) extends AnnotatorView(elem: Element, WebSocketNLPTransport("webintelligence.eu", "notebook", "guest" + Math.random() * 1000))
{

    //lazy val connector = WebSocketNLPTransport("localhost:1112", "notebook", "guest" + Math.random() * 1000)

    val server: Var[String] = Var(s"${connector.protocol}://${connector.host}/channel/${connector.channel}")

    val connected: Var[Boolean] = connector.connected

    override def bindView() = {
        super.bindView()
        connector.open()
    }

    connected.triggerLater(
        println("CONNECTION ESTABLISHED")
    )

}

*/
/*
class AnnotatorNLP(elem: Element) extends AnnotatorView(elem: Element, WebSocketNLPTransport("localhost:1112", "notebook", "guest" + Math.random() * 1000))
{

    //lazy val connector = WebSocketNLPTransport("localhost:1112", "notebook", "guest" + Math.random() * 1000)

    val server: Var[String] = Var(s"${connector.protocol}://${connector.host}/channel/${connector.channel}")

    val connected: Var[Boolean] = connector.connected

    override def bindView() = {
        super.bindView()
        connector.open()
    }

}
*/