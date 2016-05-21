package org.denigma.kappa


import java.io.{File => JFile}
import java.nio.ByteBuffer

import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.http.scaladsl.model.ws.BinaryMessage.Strict
import akka.http.scaladsl.testkit.WSProbe
import akka.stream.testkit.TestSubscriber
import akka.util.ByteString
import boopickle.DefaultBasic._
import org.denigma.kappa.messages._

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
abstract class BasicWebSocketSuite extends BasicKappaSuite with KappaPicklers {

  lazy val (host, port) = (config.getString("app.host"), config.getInt("app.port"))

  def pack(buffer:  ByteBuffer): Strict = BinaryMessage(ByteString(buffer))

  def unpack(mess: Message): Try[KappaMessage] = mess match {
    case BinaryMessage.Strict(bytes)=> Unpickle[KappaMessage].tryFromBytes(bytes.toByteBuffer)
    case other => Failure(new Exception("not a binary message"))
  }

  def checkMessage[T](wsClient: WSProbe, message: ByteBuffer)(partial: PartialFunction[KappaMessage, T]): T = {
    wsClient.sendMessage(pack(message))
    wsClient.inProbe.request(1).expectNextPF {
      case BinaryMessage.Strict(bytes) if {
        Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
          case l =>
            if(partial.isDefinedAt(l)) true else {
              println("checkProjects failed with message "+l)
              false
            }
        }
      } =>
        val value = Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer)
        partial(value)
    }
  }
  implicit protected def toMessagePartial[T](kappaPartial: PartialFunction[KappaMessage, T]): PartialFunction[Message, T] = new PartialFunction[Message, T] {
    def apply(message: Message) = kappaPartial(unpack(message).asInstanceOf[Success[KappaMessage]].get)
    def isDefinedAt(message: Message) = unpack(message) match {
      case Success(mess)=> kappaPartial.isDefinedAt(mess)
      case _ => false
    }
  }

  @tailrec final def probeLoop[T](probe: TestSubscriber.Probe[Message], message: Message, fun: PartialFunction[Message, T]): T = if(fun.isDefinedAt(message)) {
    fun(message)
  } else {
    probeLoop(probe, probe.requestNext(), fun)
  }

  @tailrec final def probeCollectUntilLoop[T](probe: TestSubscriber.Probe[Message],
                                         message: Message,
                                         collected: List[T],
                                         collect: PartialFunction[Message, T],
                                         until: PartialFunction[Message, Boolean]
                                        ): List[T] =
  if(until.isDefinedAt(message) && until(message)) {
    collected.reverse
  } else {
    val acc = if(collect.isDefinedAt(message)) collect(message)::collected else collected
    probeCollectUntilLoop(probe, probe.requestNext(), acc, collect, until)
    //probeCollectLoop(probe, probe.requestNext(), fun)
  }


/*
  protected def collectPartialKappaMessageUntil[T]
    (probe: TestSubscriber.Probe[Message], timeout: FiniteDuration = 5000 millis)
    (collect: PartialFunction[Message, T])
    (until: PartialFunction[Message, Boolean]): T =
    {
      until: PartialFunction[Message, Boolean]
    }
*/
  /*

  protected def collectPartialKappaMessageUntil[T]
    (probe: TestSubscriber.Probe[Message], timeout: FiniteDuration = 5000 millis)
    (partial: PartialFunction[KappaMessage, T]): T =
    waitPartialMessage(probe, timeout)(partial)
*/


  protected def waitPartialKappaMessage[T](probe: TestSubscriber.Probe[Message], timeout: FiniteDuration = 5000 millis)(partial: PartialFunction[KappaMessage, T]) =
    waitPartialMessage(probe, timeout)(partial)

  protected def waitPartialMessage[T](probe: TestSubscriber.Probe[Message], timeout: FiniteDuration = 5000 millis)(fun: PartialFunction[Message, T]) = {
    val future = Future {
      val result = probeLoop(probe, probe.requestNext(), fun)
      result
    }
    Await.result(future, timeout)
  }

  protected def collectPartialKappaMessage[T](probe: TestSubscriber.Probe[Message], timeout: FiniteDuration = 5000 millis)
                                        (collect: PartialFunction[KappaMessage, T])
                                        (until: PartialFunction[KappaMessage, Boolean]): List[T] = {
    val future = Future {
      val result: List[T] = probeCollectUntilLoop(probe, probe.requestNext(), List.empty[T], collect, until)
      result
    }
    Await.result(future, timeout)
  }


  protected def collectPartialMessage[T](probe: TestSubscriber.Probe[Message], timeout: FiniteDuration = 5000 millis)
                                     (collect: PartialFunction[Message, T])(until: PartialFunction[Message, Boolean]): List[T] = {
    val future = Future {
      val result: List[T] = probeCollectUntilLoop(probe, probe.requestNext(), List.empty[T], collect, until)
      result
    }
    Await.result(future, timeout)
  }

  def checkProject[T](wsClient: WSProbe, projectToLoad: KappaProject)(partial: PartialFunction[KappaMessage, T]): T =
  {
    val bytes = Pickle.intoBytes[KappaMessage](ProjectRequests.Load(projectToLoad))
    checkMessage[T](wsClient, bytes)(partial)
  }


  def checkConnection(wsClient: WSProbe): Unit = {
    isWebSocketUpgrade shouldEqual true

    wsClient.inProbe.request(1).expectNextPF {
      case BinaryMessage.Strict(bytes) if {
        Unpickle[KappaMessage].fromBytes(bytes.asByteBuffer) match {
          case c: Connected => true
          case _ => false
        }
      } =>
    }
  }

}
