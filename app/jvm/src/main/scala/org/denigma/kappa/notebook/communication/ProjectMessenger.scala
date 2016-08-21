package org.denigma.kappa.notebook.communication

import java.io.{InputStream, File => JFile}
import java.nio.ByteBuffer

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.actor.ActorPublisherMessage
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.KappaMessage.{Container, ServerCommand, ServerResponse}
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager

import scala.collection.immutable.SortedSet
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait ProjectMessenger extends Messenger {

  def fileManager: FileManager

  protected def projectMessages: PartialFunction[KappaMessage, Unit] = {

    case ProjectRequests.GetList =>
      val list: SortedSet[KappaProject] = fileManager.loadProjectSet()
      log.info("project list is: "+list.map(p=>p.folder.path))
      val d: ByteBuffer = Pickle.intoBytes[KappaMessage](ProjectResponses.ProjectList(list.toList))
      send(d)


    case ProjectRequests.Load(pro) => fileManager.loadProject(pro) match
    {
      case Some(project) =>
        val list: SortedSet[KappaProject] = fileManager.loadProjectSet().map(p=> if(p.name==project.name) project else p)
        val response = Container(ProjectResponses.ProjectList(list.toList)::ProjectResponses.LoadedProject(project)::Nil)
        val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
        send(d)

      case None =>
        //val error = ServerErrors(List(s"folder of ${pro.name} does not exist!"))
        val error = Failed(KappaProject(pro.name, saved = false), List(s"folder of ${pro.name} does not exist!"), username)
        val d = Pickle.intoBytes[KappaMessage](error)
        send(d)
    }

    case c @ ProjectRequests.Create(project, rewriteIfExists) =>
      fileManager.create(project) match {
        case Success(value) =>
          val response = org.denigma.kappa.messages.Done(c, username)
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)
        case Failure(th) =>
          log.error(s"creation of $project project FAILED with message ${th.getMessage}")
          val response = org.denigma.kappa.messages.Failed(c, List(th.getMessage), username)
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)
      }

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
      fileManager.remove(name) match {
        case Success(value) =>
          val response = org.denigma.kappa.messages.Done(r, username)
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)
        case Failure(th) =>
          val response = org.denigma.kappa.messages.Failed(r, List(th.getMessage), username)
          val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
          send(d)
      }

    case sv @ ProjectRequests.Save(project)=>
      log.error("PROJECT SAVING IS NOT YET IMPLEMENTED!")
  }
}
