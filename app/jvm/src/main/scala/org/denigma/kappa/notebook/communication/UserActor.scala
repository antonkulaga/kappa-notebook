package org.denigma.kappa.notebook.communication

import java.io.InputStream

import akka.actor.{ActorRef, Actor}
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.util.ByteString
import org.denigma.kappa.WebSim
import org.denigma.kappa.messages.{KappaPicklers, KappaMessages}
import boopickle.Default._
import org.denigma.kappa.notebook.communication.ServerMessages.Result
import org.denigma.kappa.notebook.services.WebSimClient
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern._
import java.time._
/**
  * Created by antonkulaga on 10/03/16.
  */
class UserActor(username: String, servers: ActorRef) extends KappaPicklers with  Actor with akka.actor.ActorLogging with ActorPublisher[SocketMessages.OutgoingMessage] {


  implicit def ctx = context.dispatcher

  //val servers: Map[String, WebSimClient] = Map("default", new WebSimClient()(this.context.system, ActorMaterializer())) //note: will be totally rewritten


  def readResource(path: String): Iterator[String] = {
    val stream : InputStream = getClass.getResourceAsStream(path)
    scala.io.Source.fromInputStream( stream ).getLines
  }

  override def receive: Receive = {

    case SocketMessages.IncomingMessage(channel, uname, message: BinaryMessage.Strict, time) =>
      println("something binary received")

      Unpickle[KappaMessages.Message].fromBytes(message.data.toByteBuffer) match {
        case cont: KappaMessages.Container =>
          val code = cont.code.head
          val params = cont.run.headOption.getOrElse(KappaMessages.RunParameters())
          run(code.text, params)
/*
          val result = Kappa.run(code, params)
          val data = Pickle.intoBytes[KappaMessages.Message](result)
          BinaryMessage(ByteString(data))
          */

        case KappaMessages.Load(modelName) =>
          val code = KappaMessages.Code(readResource("/examples/abc.ka").toList)
          println(s"let us load the model, it is $code")
          val d = Pickle.intoBytes[KappaMessages.Message](code)
          send(BinaryMessage(ByteString(d)))

        case code: KappaMessages.Code =>
          run(code.text, KappaMessages.RunParameters())
      }
  }

  def send(binaryMessage: BinaryMessage, channel: String = "all") = {
    val message = SocketMessages.OutgoingMessage(channel, username, binaryMessage, LocalDateTime.now)
    onNext(message)
  }

  protected def run(code: String, params: KappaMessages.RunParameters) = {
    println(s"let us run $params")
    //val toServer = WebSim.run
    val run = WebSim.RunModel(code, params.points, params.events, params.time)
    implicit val timeout = new akka.util.Timeout(30 seconds)
    val resp: Future[Result] = (servers ? ServerMessages.Run(username, "localhost", run, self, 1 seconds) ).mapTo[ServerMessages.Result]
    resp.foreach{
      case res=>
        println("result is: "+res.simulationStatus)
      //KappaMessages.KappaSeries
    }
  }
  println(s"user $username started")
}
