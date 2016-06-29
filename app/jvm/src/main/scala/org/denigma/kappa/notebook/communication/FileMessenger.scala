package org.denigma.kappa.notebook.communication

import java.io.{File => JFile}
import java.nio.ByteBuffer

import akka.stream.scaladsl.FileIO
import better.files.File
import boopickle.DefaultBasic._
import org.denigma.kappa.messages.FileResponses.RenamingResult
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.FileManager

import scala.collection.immutable.Iterable
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait FileMessenger extends Messenger {

  def fileManager: FileManager


  protected def containerMessages: PartialFunction[KappaMessage, Unit] = {
    case KappaMessage.Container(messages) =>
      messages.foreach(m=> self ! m)
  }


  protected def fileMessages: PartialFunction[KappaMessage, Unit] = {

    case r @ FileRequests.Remove(projectName, filename) =>
      fileManager.remove(projectName, filename)
      val response = org.denigma.kappa.messages.Done(r, username)
      val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
      send(d)


    case r @ FileRequests.Rename(projectName, renames, overwrite)  =>
      println("RENAMING = "+renames)
      val response: RenamingResult = fileManager.rename(projectName, renames, overwrite)
      println("RESPONSE = "+response)
      val d: ByteBuffer = Pickle.intoBytes[KappaMessage](response)
      send(d)

    case mess @ FileRequests.LoadFileSync(currentProject, path) =>
      fileManager.readBytes(currentProject, path) match {
        case Some(bytes)=>
          //println("bytes received "+bytes.length)
          //println("path = "+path)
          val m = DataMessage(mess.path, bytes)
          val d = Pickle.intoBytes[KappaMessage](m)
          send(d)

        case None =>
          val notFound = Failed(mess, List(path), username)
          val d = Pickle.intoBytes[KappaMessage](notFound)
          send(d)
      }

    case mess @ FileRequests.LoadBinaryFile(projectName, path, chunkSize) =>
      fileManager.getJavaPath(projectName, path) match {
        case Some((fl, size)) =>
          val folding: Future[Int] = FileIO.fromPath(fl, chunkSize).runFold[Int](0){
            case (acc, chunk) =>
              val downloaded = acc + chunk.length
              val mes = DataChunk(mess, path, chunk.toArray, downloaded, size)//DataMessage(path, chunk.toByteBuffer.array())
              val d = Pickle.intoBytes[KappaMessage](mes)
              //log.info("\nsend chunk for mess"+mess)
              send(d)
              downloaded
          }
          //.run(Sink.ignore)
          folding.onComplete{
            case Success(res) =>
              val mes = DataChunk(mess, path, Array(), res, size, completed = true)
              val d = Pickle.intoBytes[KappaMessage](mes)
              log.info(s"\nSEND COMPLETE: \npath = ${fl.toAbsolutePath} \nsize = $size \nmess = "+mess)
              send(d)

            case Failure(th) =>
              val d = Pickle.intoBytes[KappaMessage](Failed(mess, List(th.toString), username))
              send(d)
          }

        case None =>
          val failed = Failed(mess, errors = List(s"Path $path does not exist"), username)
          val d = Pickle.intoBytes[KappaMessage](failed)
          send(d)
      }

    case upl @ FileRequests.UploadBinary(projectName, files) =>
      files.foreach{
        case DataMessage(name, bytes) =>
          fileManager.writeBytes(projectName, name, bytes)
      }

    case upl @ FileRequests.ZipUpload(projectName, data, rewriteIfExist) =>

      val d: ByteBuffer = fileManager.uploadZiped(upl).map{
        case r =>
          Pickle.intoBytes[KappaMessage](org.denigma.kappa.messages.Done(r, username))
      }
        .getOrElse( Pickle.intoBytes[KappaMessage]{
          val resp = FileResponses.UploadStatus(projectName, data.hashCode(), rewriteIfExist)
          Failed(resp, List("Does not exist"), username)
        })
      //.map(r=>Done(r, username)).getOrElse(Failed())
      send(d)

    case FileRequests.Save(projectName, files, rewrite, false) =>
      val path: File = fileManager.root / projectName
      val rels = files.map(f=>f.relativeTo(projectName))
      val reply = FileResponses.SavedFiles(projectName, Left(rels.map(v=>v.name).toSet))
      send(Pickle.intoBytes[KappaMessage](reply))

    case FileRequests.Save(projectName, files, rewrite, returnResults) =>
      val path: File = fileManager.root / projectName
      val rels = files.map(f=>f.relativeTo(projectName))
      val kappaFiles: Map[String, KappaFile] = (for{f <- rels} yield { fileManager.writeFile(f); f.name -> f.copy(saved = true) }).toMap
      val reply = FileResponses.SavedFiles(projectName, Right(kappaFiles))
      send(Pickle.intoBytes[KappaMessage](reply))
  }

}
