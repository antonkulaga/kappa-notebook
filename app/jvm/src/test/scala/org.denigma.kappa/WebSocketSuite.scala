package org.denigma.kappa

import java.io.{File => JFile}
import java.nio.ByteBuffer

import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.BinaryMessage.Strict
import akka.http.scaladsl.testkit.WSProbe
import akka.util.ByteString
import better.files.File
import boopickle.Default._
import net.ceedubs.ficus.Ficus._
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager
import org.denigma.kappa.notebook.communication.WebSocketManager
import org.denigma.kappa.notebook.pages.WebSockets
import scala.collection.immutable._

class WebSocketSuite extends BasicKappaSuite with KappaPicklers{

  val (host, port) = (config.getString("app.host"), config.getInt("app.port"))
  val filePath: String = config.as[Option[String]]("app.files").getOrElse("files/")
  val files = File(filePath)
  files.createIfNotExists(asDirectory = true)
  val fileManager = new FileManager(files)

  val transport = new WebSocketManager(system, fileManager)

  val routes = new WebSockets(transport.openChannel).routes

  def pack(buffer:  ByteBuffer): Strict = BinaryMessage(ByteString(buffer))

  "WebSockets" should {

    "get run messages and start simulations" in {
      val wsClient = WSProbe()
      // WS creates a WebSocket request for testing
      WS("/channel/notebook?username=tester1", wsClient.flow) ~>  routes ~>
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
                              //println("console output: "+sim.simulationStatus.log_messages)
                              true
                            case _ =>
                              false
                          }        } =>
                     }
          wsClient.sendCompletion()
          //wsClient.expectCompletion()
        }
    }

    "provide errors messages for wrong models" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester2", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          isWebSocketUpgrade shouldEqual true
          wsClient.inProbe.request(1).expectNextPF{
            case BinaryMessage.Strict(bytes) if {
              Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
                case c: Connected=>  true
                case _ => false} } =>
          }

          val model = abc
            .replace("A(x),B(x)", "A(x&*&**),*(B(&**&x)")
            .replace("A(x!_,c),C(x1~u)", "zafzafA(x!_,c),azfC(x1~u)") //note: right now sees only one error
          val params = messages.LaunchModel("", RunModel(model, Some(1000), max_events = Some(10000)))
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](params)
          wsClient.sendMessage(pack(d))

          wsClient.inProbe.request(1).expectNextPF{
            case BinaryMessage.Strict(bytes)  if {
              val mes = Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer)
              mes match {
                case SyntaxErrors(server, errors, _)=>
                  //println("expected errors are: "+ errors)
                  true
                case _=> false
              }
            } =>

          }
         wsClient.sendCompletion()
         //wsClient.expectCompletion()
        }
    }

    "load projects" in {
      val wsClient = WSProbe()
      WS("/channel/notebook?username=tester3", wsClient.flow) ~>  routes ~>
        check {
          // check response for WS Upgrade headers
          isWebSocketUpgrade shouldEqual true

          wsClient.inProbe.request(1).expectNextPF{
            case BinaryMessage.Strict(bytes) if {
              Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
                case c: Connected=>  true
                case _ => false} } =>
          }

          val message: ByteBuffer = Pickle.intoBytes[KappaMessage](Load(KappaProject("big")))
          wsClient.sendMessage(pack(message))

          wsClient.inProbe.request(1).expectNextPF{
            case BinaryMessage.Strict(bytes) if {
              Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
                case Loaded(proj: KappaProject, one::two::Nil)=>
                  proj.name shouldEqual "big"
                  proj.folder.files.map(_.name) shouldEqual Set("big_0.ka", "big_1.ka", "big_2.ka")
                  //println("something received: \n" + smth)
                  true
                case other => println("failure with "+other)
                  false}
            } =>
          }

        }
        wsClient.sendCompletion()
    }

  }


}
