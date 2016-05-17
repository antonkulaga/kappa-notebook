package org.denigma.kappa.notebook.communication

import java.io.{InputStream, File => JFile}
import java.nio.ByteBuffer
import java.time._

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import akka.util.ByteString
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.FileResponses.FileNotFound
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager
import org.denigma.kappa.notebook.communication.SocketMessages.OutgoingMessage

import scala.annotation.tailrec
import scala.collection.immutable.SortedSet
import scala.concurrent.duration._

class UserActor(username: String, servers: ActorRef, fileManager: FileManager) extends KappaPicklers
  with Actor
  with akka.actor.ActorLogging
  with ActorPublisher[SocketMessages.OutgoingMessage]{

  implicit def ctx = context.dispatcher

  val MaxBufferSize = 100
  var buf = Vector.empty[OutgoingMessage]

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      /*
      * totalDemand is a Long and could be larger than
      * what buf.splitAt can accept
      */
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }

  //val servers: Map[String, WebSimClient] = Map("default", new WebSimClient()(this.context.system, ActorMaterializer())) //note: will be totally rewritten


  def readResource(path: String): Iterator[String] = {
    val stream: InputStream = getClass.getResourceAsStream(path)
    scala.io.Source.fromInputStream( stream ).getLines
  }


  def run(params: RunModel): Unit = {
    val toServer = RunAtServer(username, "localhost", params, self, 100 millis)
    //println("RUN QT SERVER: "+toServer)
    servers ! toServer
  }


  protected def onTextMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, TextMessage.Strict(text), time) =>
    /*
      val dec = parser.decode[WebSim.KappaMessage](text)
      dec match {
        case Xor.Right(value) => value match {
          //case status: WebSim.SimulationStatus =>
          case params: WebSim.RunModel =>
            run(params)
          case other => println(s"some other WebSim: \n $other")
        }
        case Xor.Left(failure) =>
          log.error(s"JSON failure detectedc $failure")
      }
      */
  }

  protected def onBinaryMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, message: BinaryMessage.Strict, time) =>
      Unpickle[KappaMessage].fromBytes(message.data.toByteBuffer) match
      {

        case mess @ FileRequests.LoadFileSync(path) =>
          fileManager.readBytes(path) match {
            case Some(bytes)=>
              println("bytes received "+bytes.length)
              val m = DataMessage(mess.path, bytes)
              val d = Pickle.intoBytes[KappaMessage](m)
              send(d)

            case None =>
              val notFound = FileNotFound(path)
              val d = Pickle.intoBytes(notFound)
              send(d)
          }

        case mess @ FileRequests.LoadFile(path) =>
          fileManager.getJavaFile(path) match {
            case Some(fl) =>
            case None =>
              val failed = Failed(mess, username, error = List(s"Path $path does not exist"))
              val d = Pickle.intoBytes(failed)
              send(d)
          }

        case ProjectRequests.Load(pro) =>
          fileManager.loadProject(pro) match {
            case project: KappaProject if project.saved =>
              val list: SortedSet[KappaProject] = fileManager.loadProjectSet().map(p=> if(p.name==project.name) project else p)
              val response = ProjectResponses.Loaded(Some(project), list)
              val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
              //log.info("################################"+response)
              send(d)

            case project =>
              //val error = ServerErrors(List(s"folder of ${pro.name} does not exist!"))
              val error = Failed(project, List(s"folder of ${pro.name} does not exist!"), username)
              val d = Pickle.intoBytes[KappaMessage](error)

              //log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!"+error)
              send(d)
          }

        case r @ FileRequests.Remove(name) =>
          fileManager.remove(name)
          val response = Done(r, username)
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)


        case c @ FileRequests.Create(project, rewriteIfExists) =>
          fileManager.create(project)
          val response = Done(c, username)
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)

        case dn @ FileRequests.Download(projectName)=>
          //log.info("DOWNLOADED STARTED "+projectName)

          fileManager.loadZiped(projectName) match
          {
            case Some(response: FileResponses.Downloaded) =>
              log.info("RESPONDE = "+response)
              val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
              send(d)

            case None =>
              val response = Failed(dn, List(s"project $projectName does not exist"), username)
              println(response)
              val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
              send(d)
          }

        case upl @ FileRequests.ZipUpload(projectName, data, rewriteIfExist) =>

          val d: ByteBuffer = fileManager.uploadZiped(upl).map{
            case r =>
              Pickle.intoBytes[KappaMessage](Done(r, username))
          }
            .getOrElse( Pickle.intoBytes[KappaMessage]{
              val resp = FileResponses.UploadStatus(projectName, data.hashCode(), rewriteIfExist)
              Failed(resp, List("Does not exist"), username)
            })
          //.map(r=>Done(r, username)).getOrElse(Failed())
          send(d)


        case sv @ ProjectRequests.Save(project)=>
          println("SAVING IS NOT YET IMPLEMENTED!")

        case LaunchModel(server, parameters, counter)=>

          run(parameters)

        case other => log.error(s"unexpected $other")
      }
    //log.error(s"something binary received on $channel by $username")
  }

  protected def onServerMessage: Receive = {

    case result: SimulationResult =>
      val d = Pickle.intoBytes[KappaMessage](result)
      send(d)

    case s: SyntaxErrors=>
      val d = Pickle.intoBytes[KappaMessage](s)
      send(d)

    case result: Connected =>
      val d = Pickle.intoBytes[KappaMessage](result)
      send(d)

    case Disconnected(user, channel, list) =>
      log.info(s"User $user disconnected from channel $channel")



  }

  protected def onOtherMessage: Receive = {

    case ActorPublisherMessage.Request(n) => deliverBuf()

    case other => log.error(s"Unknown other message: $other")
  }


  override def receive: Receive =  onTextMessage.orElse(onBinaryMessage).orElse(onServerMessage).orElse(onOtherMessage)

  def deliver(mess: OutgoingMessage) = {
    if (buf.isEmpty && totalDemand > 0)
      onNext(mess)
    else {
      buf :+= mess
      deliverBuf()
    }
  }

  def send(textMessage: TextMessage, channel: String): Unit = {
    val message = SocketMessages.OutgoingMessage(channel, username, textMessage, LocalDateTime.now)
    deliver(message)
  }

  def sendBinary(binaryMessage: BinaryMessage, channel: String = "all") = {
    val message = SocketMessages.OutgoingMessage(channel, username, binaryMessage, LocalDateTime.now)
    deliver(message)
  }

  def send(d: ByteBuffer, channel: String = "all"): Unit = {
    sendBinary(BinaryMessage(ByteString(d)), channel)
  }

}
