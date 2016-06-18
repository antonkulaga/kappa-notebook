package org.denigma.kappa.notebook.communication

import java.io.{InputStream, File => JFile}
import java.nio.ByteBuffer

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.actor.ActorPublisherMessage
import boopickle.DefaultBasic._
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager

import scala.collection.immutable.SortedSet
import scala.concurrent.duration._

class UserActor(val username: String, servers: ActorRef, val fileManager: FileManager) extends FileMessenger
{

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
  }

  protected def projectMessages: PartialFunction[KappaMessage, Unit] = {
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

    case c @ ProjectRequests.Create(project, rewriteIfExists) =>
      fileManager.create(project)
      val response = org.denigma.kappa.messages.Done(c, username)
      val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
      send(d)

    case dn @ ProjectRequests.Download(projectName)=>
      //log.info("DOWNLOADED STARTED "+projectName)

      fileManager.loadZiped(projectName) match
      {
        case Some(response: FileResponses.Downloaded) =>
          //log.info("RESPONDE = "+response)
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)

        case None =>
          val response = Failed(dn, List(s"project $projectName does not exist"), username)
          println(response)
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)
      }

    case r @ ProjectRequests.Remove(name) =>
      fileManager.remove(name)
      val response = org.denigma.kappa.messages.Done(r, username)
      val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
      send(d)

  }

  protected def simulationMessages: Receive  = {
    case LaunchModel(server, parameters, counter)=> run(parameters)
  }

  protected def otherKappaMessages: Receive  = {
    case other => log.error(s"unexpected $other")
  }


  protected def onBinaryMessage: Receive = {
    case SocketMessages.IncomingMessage(channel, uname, message: BinaryMessage.Strict, time) =>
      val mes = Unpickle[KappaMessage].fromBytes(message.data.toByteBuffer)
      val fun = fileMessages.orElse(projectMessages).orElse(simulationMessages).orElse(otherKappaMessages)
      fun(mes)
    //log.error(s"something binary received on $channel by $username")
  }

  protected def onServerMessage: Receive = {

    case result: SimulationResult =>
      val d = Pickle.intoBytes[KappaMessage](KappaMessage.ServerResponse(result))
      send(d)

    case s: SyntaxErrors=>
      val d = Pickle.intoBytes[KappaMessage](KappaMessage.ServerResponse(s))
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


}
