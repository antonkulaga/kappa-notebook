package org.denigma.kappa

import java.nio.ByteBuffer

import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.BinaryMessage.Strict
import akka.http.scaladsl.testkit.WSProbe
import akka.util.ByteString
import org.denigma.kappa.notebook.Router
import boopickle.Default._
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.communication.WebSocketManager
import org.denigma.kappa.notebook.pages.WebSockets

class WebSocketSuite extends BasicKappaSuite with KappaPicklers{

  val transport = new WebSocketManager(system)

  val  routes = new WebSockets(transport.openChannel).routes

  def pack(buffer:  ByteBuffer): Strict = BinaryMessage(ByteString(buffer))

  "WebSockets" should {

    "get run messages and start simulations" in {
      val wsClient = WSProbe()
      // WS creates a WebSocket request for testing
      WS("/channel/notebook?username=tester", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          isWebSocketUpgrade shouldEqual true
          val params = RunModel(abc, Some(1000), max_events = Some(10000))
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](LaunchModel("", params))
          wsClient.sendMessage(pack(d))
          wsClient.inProbe.request(1).expectNextPF{
            case BinaryMessage.Strict(bytes) if {
              Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
                case c: Connected=>  true
                case _ => false} } =>
          }
          wsClient.inProbe.request(1).expectNextPF{
                        case BinaryMessage.Strict(bytes) if {
                          Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
                            case sim: SimulationResult=>
                              println("console output: "+sim.simulationStatus.log_messages)
                              true
                            case _ =>
                              false
                          }        } =>
                     }
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
          val params = messages.LaunchModel("", RunModel(model, Some(1000), max_events = Some(10000)))
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](params)
          wsClient.sendMessage(pack(d))
          wsClient.inProbe.request(1).expectNextPF{
            case BinaryMessage.Strict(bytes) if {
              Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
                case c: Connected=>  true
                case _ => false} } =>
          }

          wsClient.inProbe.request(1).expectNextPF{
            case BinaryMessage.Strict(bytes)  if {
              val mes = Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer)
              mes match {
                case SyntaxErrors(server, errors, _)=>
                  println("expected errors are: "+ errors)
                  true
                case _=> false
              }
            } =>

          }
          //wsClient.sendCompletion()
          //wsClient.expectCompletion()
        }
    }

    "provide a list of files" in {


    }

    }
  // tests:
  // create a testing probe representing the client-side


}
