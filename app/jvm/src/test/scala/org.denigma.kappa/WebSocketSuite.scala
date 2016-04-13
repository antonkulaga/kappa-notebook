package org.denigma.kappa

import java.nio.ByteBuffer

import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.testkit.WSProbe
import akka.util.ByteString
import org.denigma.kappa.WebSim._
import org.denigma.kappa.notebook.Router
import boopickle.Default._
import org.denigma.kappa.messages.{RunModel, WebSimMessage, WebSimPicklers}
import org.denigma.kappa.notebook.communication.WebSocketManager
import org.denigma.kappa.notebook.pages.WebSockets

class WebSocketSuite extends BasicKappaSuite with WebSimPicklers{

  val transport = new WebSocketManager(system)

  val  routes = new WebSockets(transport.openChannel).routes

  def pack(buffer:  ByteBuffer) = BinaryMessage(ByteString(buffer))

  "WebSockets" should {
    "get run messages and start simulations" in {
      val wsClient = WSProbe()
      // WS creates a WebSocket request for testing
      WS("/channel/notebook?username=tester", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          isWebSocketUpgrade shouldEqual true
          //val code = WebSim.Code(abc)
          val params = RunModel(abc, 1000, max_events = Some(10000))
          val d: ByteBuffer = Pickle.intoBytes[WebSimMessage](params)
          wsClient.sendMessage(pack(d))

          wsClient.inProbe.request(1).expectNextPF{
            case BinaryMessage.Strict(bytes)  =>
              val mes = Unpickle[WebSimMessage].fromBytes(bytes.asByteBuffer)
              mes match {
                case sim: SimulationResult=>
                  println("console output: "+sim.simulationStatus.logMessages)
              }
          }
          //wsClient.sendCompletion()
          //wsClient.expectCompletion()
        }
    }
    "provide errors messages for wrong models" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          isWebSocketUpgrade shouldEqual true
          val model = abc
            .replace("A(x),B(x)", "A(x&*&**),*(B(&**&x)")
            .replace("A(x!_,c),C(x1~u)", "zafzafA(x!_,c),azfC(x1~u)") //note: right now sees only one error
          val params = messages.RunModel(model, 1000, max_events = Some(10000))
          val d: ByteBuffer = Pickle.intoBytes[WebSimMessage](params)
          wsClient.sendMessage(pack(d))

          wsClient.inProbe.request(1).expectNextPF{
            case BinaryMessage.Strict(bytes)  =>
              val mes = Unpickle[WebSimMessage].fromBytes(bytes.asByteBuffer)
              mes match {
                case SyntaxErrors(server, errors, params)=>
                  println("expected errors are: "+ errors)
              }
          }
          //wsClient.sendCompletion()
          //wsClient.expectCompletion()
        }
    }

  }
  // tests:
  // create a testing probe representing the client-side


}
