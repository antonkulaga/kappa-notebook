package org.denigma.kappa.notebook.communication

import java.io.{File => JFile, InputStream}
import java.nio.ByteBuffer

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.actor.ActorPublisherMessage
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.KappaMessage.{ServerCommand, ServerResponse}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager

import scala.collection.immutable.SortedSet
import scala.concurrent.duration._

trait ProjectMessenger extends Messenger {

  def fileManager: FileManager

  protected def projectMessages: PartialFunction[KappaMessage, Unit] = {
    case ProjectRequests.Load(pro) => fileManager.loadProject(pro) match
    {
      case project: KappaProject if project.saved =>
        val list: SortedSet[KappaProject] = fileManager.loadProjectSet().map(p=> if(p.name==project.name) project else p)
        val response = ProjectResponses.Loaded(Some(project), list)
        val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
        send(d)

      case project =>
        //val error = ServerErrors(List(s"folder of ${pro.name} does not exist!"))
        val error = Failed(project, List(s"folder of ${pro.name} does not exist!"), username)
        val d = Pickle.intoBytes[KappaMessage](error)
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
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)

        case None =>
          val response = Failed(dn, List(s"project $projectName does not exist"), username)
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)
      }

    case r @ ProjectRequests.Remove(name) =>
      fileManager.remove(name)
      val response = org.denigma.kappa.messages.Done(r, username)
      val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
      send(d)

    case sv @ ProjectRequests.Save(project)=>
      println("PROJECT SAVING IS NOT YET IMPLEMENTED!")
  }
}
